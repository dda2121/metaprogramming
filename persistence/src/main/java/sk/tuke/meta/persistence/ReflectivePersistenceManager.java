package sk.tuke.meta.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.tuke.meta.persistence.exception.FieldAccessException;
import sk.tuke.meta.persistence.exception.MissedIdException;
import sk.tuke.meta.persistence.util.Util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static sk.tuke.meta.persistence.util.SQLUtil.typeToSQL;
import static sk.tuke.meta.persistence.util.Util.castToString;
import static sk.tuke.meta.persistence.util.Util.getClassNameWithoutPackage;


public class ReflectivePersistenceManager implements PersistenceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectivePersistenceManager.class.getName());

    private final Connection connection;

    public ReflectivePersistenceManager(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTables(Class<?>... types) {
        for (Class c: types) {
            Field[] fields = c.getDeclaredFields();
            String tableName = getClassNameWithoutPackage(c).toLowerCase();
            String query = "CREATE TABLE IF NOT EXISTS " + tableName + getTableScript(fields);
            try {
                Statement statement = connection.createStatement();
                statement.execute(query);
                LOGGER.info("Table '" + tableName + "' was successfully created.");
            } catch (SQLException e) {
                LOGGER.error("Error occurred when creating table " + tableName + ": " + e.getMessage());
            }
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> type, long id) {
        String query = "SELECT * FROM " + getClassNameWithoutPackage(type).toLowerCase()
                + " WHERE id = " + id;
        ResultSet rs;
        try {
            Statement statement = connection.createStatement();
            rs = statement.executeQuery(query);
        } catch (SQLException e) {
            LOGGER.error("Error occurred when retrieving object from a database with class type '" +
                    getClassNameWithoutPackage(type) + "'.");
            return Optional.empty();
        }
        try {
            if (rs.next()) {
                return processResultSet(type, rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error occurred when processing result set of an object with class type '" +
                    getClassNameWithoutPackage(type) + "'.");
        }
        return Optional.empty();
    }

    private <T> Optional<T> processResultSet(Class<T> type, ResultSet rs) {
        try {
            return Optional.of(resultSetToObject(type, rs));
        } catch (NoSuchMethodException e) {
            LOGGER.error("Provided object with class type '" + getClassNameWithoutPackage(type) +
                    "' doesn't contain empty constructor.");
        } catch (InvocationTargetException e) {
            LOGGER.error("Error occurred when creating instance of an class '" +
                    getClassNameWithoutPackage(type) + "'.");
        } catch (InstantiationException e) {
            LOGGER.error("Error occurred when creating an instance of an abstract class '" +
                    getClassNameWithoutPackage(type) + "'.");
        } catch (IllegalAccessException e) {
            LOGGER.error("Error occurred due to inaccessible constructor in class '" +
                    getClassNameWithoutPackage(type) + "'.");
        } catch (SQLException e) {
            LOGGER.error("Error processing result set: Please check the validity of the result set " +
                    "and the provided column label in class '" + getClassNameWithoutPackage(type) + "'.");
        }
        return Optional.empty();
    }

    private <T> T resultSetToObject(Class<T> type, ResultSet rs) throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException, SQLException {
        T obj = type.getDeclaredConstructor().newInstance();
        Field[] fields = type.getDeclaredFields();
        for (Field f: fields) {
            f.setAccessible(true);
            String value = rs.getString(f.getName());
            f.set(obj, Util.convertDataType(f.getType(), value, connection));
        }
        return obj;
    }

    @Override
    public <T> List<T> getAll(Class<T> type) {
        return Collections.emptyList();
    }

    @Override
    public void save(Object entity) {
        String className = getClassNameWithoutPackage(entity.getClass());
        Map<String, Object> values;
        try {
            values = getValues(entity);
        } catch (FieldAccessException e) {
            LOGGER.error(e.getMessage());
            return;
        }

        if (!values.containsKey("id")) {
            LOGGER.error("Provided object with type " + className + " doesn't contain 'id' field");
            return;
        }

        // TODO add validation if id is not number
        if ((Long) values.get("id") == 0) {
            handleInsert(className, values, entity);
        } else {
            handleUpdate(className, values);
        }
    }

    @Override
    public void delete(Object entity) {
    }

    private String getTableScript(Field[] fields) {
        StringBuilder columns = new StringBuilder("(");
        for (Field f: fields) {
            f.setAccessible(true);
            columns.append(f.getName()).append(" ");
            columns.append(typeToSQL(f.getType()));
            if (f.getName().equals("id")) {
                columns.append(" PRIMARY KEY,");
            } else {
                columns.append(",");
            }
        }
        if (columns.toString().endsWith(",")) {
            columns = new StringBuilder(columns.substring(0, columns.length() - 1));
        }
        columns.append(");");
        return columns.toString();
    }

    private void handleInsert(String className, Map<String, Object> values, Object entity) {
        try {
            long id = insert(className.toLowerCase(), values);
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.setLong(entity, id);
            LOGGER.info("Raw was successfully inserted into " + className.toLowerCase() + " table.");
        } catch (SQLException e) {
            LOGGER.error("Error occurred when inserting row into table '" + className.toLowerCase() + "': " + e.getMessage());
        } catch (NoSuchFieldException e) {
            LOGGER.error("Object with type " + className + " doesn't contain 'id' field");
        } catch (IllegalAccessException e) {
            LOGGER.error("Error occurred when setting id to newly create object with type " + className);
        } catch (MissedIdException e) {
            LOGGER.error("Error occurred when inserting raw into a table " + className.toLowerCase() +
                    ". Non primitive object that is behave as FK has not set up ID field");
        }
    }

    // TODO id can be other type then long
    private void handleUpdate(String className, Map<String, Object> values) {
        try {
            update(className, values);
            LOGGER.info("Raw was successfully updated in " + className.toLowerCase() + " table.");
        } catch (SQLException e) {
            LOGGER.error("Error occurred when updating row in table '" + className.toLowerCase() + "': " + e.getMessage());
        } catch (MissedIdException e) {
            LOGGER.error("Error occurred when inserting raw into a table " + className.toLowerCase() +
                    ". Non primitive object that is behave as FK has not set up ID field");
        } catch (NoSuchFieldException e) {
            LOGGER.error("Object with type " + className + " doesn't contain 'id' field");
        } catch (IllegalAccessException e) {
            LOGGER.error("Error occurred when retrieving 'id' value from object with type " + className);
        }
    }


    // TODO id can be other type then long
    private long insert(String tableName, Map<String, Object> data) throws SQLException, MissedIdException, NoSuchFieldException, IllegalAccessException {
        String query = getInsertQuery(tableName, data);

        Statement statement = connection.createStatement();
        int affectedRows = statement.executeUpdate(query);

        if (affectedRows == 0) {
            throw new SQLException("Creating " + tableName + " failed, no rows affected.");
        }

        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            }
            else {
                throw new SQLException("Creating " + tableName + " failed, no ID obtained.");
            }
        }
    }

    private void update(String tableName, Map<String, Object> values) throws SQLException, MissedIdException, NoSuchFieldException, IllegalAccessException {
        String query = getUpdateQuery(tableName, values);

        Statement statement = connection.createStatement();
        statement.executeUpdate(query);
    }

    private Map<String, Object> getValues(Object obj) throws FieldAccessException {
        Class cls = obj.getClass();
        Field[] fields = cls.getDeclaredFields();
        Map<String, Object> values = new HashMap<>();
        for (Field field: fields) {
            field.setAccessible(true);
            try {
                values.put(field.getName(), field.get(obj));
            } catch (IllegalAccessException e) {
                throw new FieldAccessException("Cannot access field '" + field.getName() + "' in class " + cls.getName());
            }
        }
        return values;
    }

    private String getInsertQuery(String tableName, Map<String, Object> data) throws MissedIdException, IllegalAccessException, NoSuchFieldException {
        StringBuilder names = new StringBuilder("(");
        StringBuilder values = new StringBuilder("(");
        for (String key: data.keySet()) {
            if (key.equals("id")) {
                continue;
            }
            String val = castToString(data.get(key));
            if (val != null) {
                names.append(key).append(",");
                if (data.get(key).getClass().equals(String.class)) {
                    values.append("\"").append(val).append("\",");
                } else {
                    values.append(val).append(",");
                }
            }
        }
        names = new StringBuilder(names.substring(0, names.length() - 1));
        values = new StringBuilder(values.substring(0, values.length() - 1));
        names.append(")");
        values.append(")");
        return "INSERT INTO " + tableName + names + " values" + values + ";";
    }

    private String getUpdateQuery(String tableName, Map<String, Object> data) throws MissedIdException, NoSuchFieldException, IllegalAccessException {
        StringBuilder query = new StringBuilder("UPDATE " + tableName + " SET ");

        for (String key: data.keySet()) {
            if (key.equals("id")) {
                continue;
            }
            String val = castToString(data.get(key));
            if (val != null) {
                if (data.get(key).getClass().equals(String.class)) {
                    query.append(key).append("=\"").append(val).append("\",");
                } else {
                    query.append(key).append("=").append(val).append(",");
                }
            } else {
                query.append(key).append("=").append((String) null).append(",");
            }
        }
        query = new StringBuilder(query.substring(0, query.length() - 1));
        query.append(" WHERE id = ");
        Object idVal = data.get("id");
        if (idVal.getClass().equals(String.class)) {
            query.append("\"").append(idVal).append("\";");
        } else {
            query.append(idVal).append(";");
        }
        return query.toString();
    }
}
