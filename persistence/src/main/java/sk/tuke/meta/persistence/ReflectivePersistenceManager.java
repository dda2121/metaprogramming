package sk.tuke.meta.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.tuke.meta.persistence.exception.FieldAccessException;
import sk.tuke.meta.persistence.exception.MissedIdException;
import sk.tuke.meta.persistence.util.Util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;

import static sk.tuke.meta.persistence.util.SQLUtil.getObjectIdValue;
import static sk.tuke.meta.persistence.util.SQLUtil.typeToSQL;
import static sk.tuke.meta.persistence.util.Util.castToObject;
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
            String query = "CREATE TABLE IF NOT EXISTS [" + tableName + "]" + getTableScript(fields);
            try {
                Statement statement = connection.createStatement();
                statement.execute(query);
                LOGGER.info("Table '" + tableName + "' was successfully created.");
            } catch (SQLException e) {
                throw new PersistenceException("Error occurred when creating table " + tableName + ": " + e.getMessage());
            }
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> type, long id) {
        String query = "SELECT * FROM [" + getClassNameWithoutPackage(type).toLowerCase()
                + "] WHERE [id] = " + id;
        ResultSet rs;
        try {
            Statement statement = connection.createStatement();
            rs = statement.executeQuery(query);
        } catch (SQLException e) {
            throw new PersistenceException("Error occurred when retrieving object from a database with class type '" +
                    getClassNameWithoutPackage(type) + "'.");
        }
        try {
            if (rs.next()) {
                return processResultSet(type, rs);
            }
        } catch (PersistenceException | SQLException e) {
            throw new PersistenceException(e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> getAll(Class<T> type) {
        List<T> result = new ArrayList<>();
        String query = "SELECT * FROM [" + getClassNameWithoutPackage(type).toLowerCase() + "]";
        ResultSet rs;
        try {
            Statement statement = connection.createStatement();
            rs = statement.executeQuery(query);
        } catch (SQLException e) {
            throw new PersistenceException("Error occurred when retrieving objects from a database with class type '" +
                    getClassNameWithoutPackage(type) + "'.");
        }

        try {
            while (rs.next()) {
                Optional<T> optional = processResultSet(type, rs);
                optional.ifPresent(result::add);
            }
            return result;
        } catch (PersistenceException | SQLException e) {
            throw new PersistenceException(e.getMessage());
        }
    }

    @Override
    public void save(Object entity) {
        String className = getClassNameWithoutPackage(entity.getClass());
        Map<String, Object> values;
        try {
            values = getValues(entity);
        } catch (FieldAccessException e) {
            throw new PersistenceException(e.getMessage());
        }

        if (!values.containsKey("id")) {
            throw new PersistenceException("Provided object with type " + className + " doesn't contain 'id' field");
        }

        try {
            if ((Long) values.get("id") == 0) {
                handleInsert(className, values, entity);
            } else {
                handleUpdate(className, values);
            }
        } catch (PersistenceException e) {
            throw new PersistenceException(e.getMessage());
        }
    }

    @Override
    public void delete(Object entity) {
        String className = getClassNameWithoutPackage(entity.getClass());
        Long id;
        try {
            id = getObjectIdValue(entity);
            if (id == null || id == 0) {
                throw new PersistenceException("Object with class type '" + className +
                        "' has not set up 'id' field. Not proceeding with deleting.");
            }
        } catch (NoSuchFieldException e) {
            throw new PersistenceException("Provided object with class type '" + className + "' doesn't contain 'id' field.");
        } catch (IllegalAccessException e) {
            throw new PersistenceException("Error occurred when accessing 'id' field of an object with class type '" + className);
        }

        String query = "DELETE FROM [" + className.toLowerCase() + "] WHERE [id] = " + id;
        try {
            Statement statement = connection.createStatement();
            statement.execute(query);
        } catch (SQLException e) {
            throw new PersistenceException("Error occurred when deleting raw from a table " + className.toLowerCase() + "': "
                    + e.getMessage());
        }
        LOGGER.info("Raw was successfully deleted from '" + className.toLowerCase() + "' table");
    }

    private <T> Optional<T> processResultSet(Class<T> type, ResultSet rs) throws PersistenceException {
        try {
            return Optional.of(resultSetToObject(type, rs));
        } catch (NoSuchMethodException e) {
            throw new PersistenceException("Provided object with class type '" + getClassNameWithoutPackage(type) +
                    "' doesn't contain empty constructor.");
        } catch (InvocationTargetException e) {
            throw new PersistenceException("Error occurred when creating instance of an class '" +
                    getClassNameWithoutPackage(type) + "'.");
        } catch (InstantiationException e) {
            throw new PersistenceException("Error occurred when creating an instance of an abstract class '" +
                    getClassNameWithoutPackage(type) + "'.");
        } catch (IllegalAccessException e) {
            throw new PersistenceException("Error occurred due to inaccessible constructor in class '" +
                    getClassNameWithoutPackage(type) + "'.");
        } catch (SQLException e) {
            throw new PersistenceException("Error processing result set: Please check the validity of the result set " +
                    "and the provided column label in class '" + getClassNameWithoutPackage(type) + "'.");
        }
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

    private String getTableScript(Field[] fields) {
        StringBuilder columns = new StringBuilder("(");
        for (Field f: fields) {
            f.setAccessible(true);
            columns.append("[").append(f.getName()).append("] ");
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

    private void handleInsert(String className, Map<String, Object> values, Object entity) throws PersistenceException {
        try {
            long id = insert(className.toLowerCase(), values);
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.setLong(entity, id);
            LOGGER.info("Raw was successfully inserted into '" + className.toLowerCase() + "' table.");
        } catch (SQLException e) {
            throw new PersistenceException("Error occurred when inserting row into table '" + className.toLowerCase() + "': " + e.getMessage());
        } catch (NoSuchFieldException e) {
            throw new PersistenceException("Object with type " + className + " doesn't contain 'id' field");
        } catch (IllegalAccessException e) {
            throw new PersistenceException("Error occurred when setting id to newly create object with type " + className);
        } catch (MissedIdException e) {
            throw new PersistenceException("Error occurred when inserting raw into a table " + className.toLowerCase() +
                    ". Non primitive object that is behave as FK has not set up ID field");
        }
    }

    private void handleUpdate(String className, Map<String, Object> values) throws PersistenceException {
        try {
            update(className, values);
            LOGGER.info("Raw was successfully updated in '" + className.toLowerCase() + "' table.");
        } catch (SQLException e) {
            throw new PersistenceException("Error occurred when updating row in table '" + className.toLowerCase() + "': " + e.getMessage());
        } catch (MissedIdException e) {
            throw new PersistenceException("Error occurred when inserting raw into a table " + className.toLowerCase() +
                    ". Non primitive object that is behave as FK has not set up ID field");
        } catch (NoSuchFieldException e) {
            throw new PersistenceException("Object with type " + className + " doesn't contain 'id' field");
        } catch (IllegalAccessException e) {
            throw new PersistenceException("Error occurred when retrieving 'id' value from object with type " + className);
        }
    }

    private long insert(String tableName, Map<String, Object> data) throws SQLException, MissedIdException, NoSuchFieldException, IllegalAccessException {
        String query = getInsertQuery(tableName, data);

        PreparedStatement statement = connection.prepareStatement(query);
        int i = 1;
        for (String key: data.keySet()) {
            if (key.equals("id")) {
                continue;
            }
            statement.setObject(i++, castToObject(data.get(key)));
        }
        int affectedRows = statement.executeUpdate();

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

        PreparedStatement statement = connection.prepareStatement(query);
        int i = 1;
        for (String key: values.keySet()) {
            if (key.equals("id")) {
                continue;
            }
            statement.setObject(i++, castToObject(values.get(key)));
        }
        statement.setObject(i, castToObject(values.get("id")));

        statement.executeUpdate();
    }

    private Map<String, Object> getValues(Object obj) throws FieldAccessException {
        Class cls = obj.getClass();
        Field[] fields = cls.getDeclaredFields();
        Map<String, Object> values = new LinkedHashMap<>();
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

    private String getInsertQuery(String tableName, Map<String, Object> data) {
        StringBuilder names = new StringBuilder("(");
        StringBuilder values = new StringBuilder("(");
        for (String key: data.keySet()) {
            if (key.equals("id")) {
                continue;
            }
            names.append("[").append(key).append("]").append(",");
            values.append("?,");
        }
        names = new StringBuilder(names.substring(0, names.length() - 1));
        values = new StringBuilder(values.substring(0, values.length() - 1));
        names.append(")");
        values.append(")");
        return "INSERT INTO [" + tableName + "]" + names + " values" + values + ";";
    }

    private String getUpdateQuery(String tableName, Map<String, Object> data) {
        StringBuilder query = new StringBuilder("UPDATE [" + tableName + "] SET ");

        for (String key: data.keySet()) {
            if (key.equals("id")) {
                continue;
            }
            query.append("[").append(key).append("]").append("=?,");
        }
        query = new StringBuilder(query.substring(0, query.length() - 1));
        query.append(" WHERE [id] = ?;");
        return query.toString();
    }
}
