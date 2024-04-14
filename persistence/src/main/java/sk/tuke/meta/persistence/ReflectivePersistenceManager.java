package sk.tuke.meta.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.exception.FieldAccessException;
import sk.tuke.meta.persistence.exception.MissedIdException;
import sk.tuke.meta.persistence.handler.LazyFetchingHandler;
import sk.tuke.meta.persistence.model.Property;
import sk.tuke.meta.persistence.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

import static sk.tuke.meta.persistence.util.Util.castToObject;
import static sk.tuke.meta.persistence.util.Util.getClassNameWithoutPackage;
import static sk.tuke.meta.persistence.util.Util.getObjectIdValue;


public class ReflectivePersistenceManager implements PersistenceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectivePersistenceManager.class.getName());

    private final Connection connection;

    public ReflectivePersistenceManager(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTables(Class<?>... types) {
        ClassLoader classLoader = ReflectivePersistenceManager.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("script.sql");
        if (inputStream != null) {
            executeScript(inputStream);
        } else {
            System.err.println("File not found!");
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> type, long id) {
        String tableName = Util.getTableName(type);
        String query = "SELECT * FROM [" + tableName + "] WHERE [" + Util.getPrimaryKeyFieldName(type) + "] = " + id;
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
        String tableName = Util.getTableName(type);
        String query = "SELECT * FROM [" + tableName + "]";
        ResultSet rs;
        try {
            Statement statement = connection.createStatement();
            rs = statement.executeQuery(query);
        } catch (SQLException e) {
            throw new PersistenceException("Error occurred when retrieving objects from a database with class type '" +
                    tableName + "'.");
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
        Class<?> cls = entity instanceof Proxy ? ((LazyFetchingHandler) Proxy.getInvocationHandler(entity)).getTarget()
                : entity.getClass();
        String tableName = Util.getTableName(cls);
        List<Property> values;
        try {
            entity = entity instanceof Proxy ? ((LazyFetchingHandler) Proxy.getInvocationHandler(entity)).getTargetObj()
                    : entity;
            values = getValues(entity);
        } catch (FieldAccessException e) {
            throw new PersistenceException(e.getMessage());
        }

        if (!Util.containsPrimaryKeyField(values)) {
            throw new PersistenceException("Provided object with type " +
                    getClassNameWithoutPackage(entity.getClass()) + " doesn't contain 'id' field");
        }

        try {
            if ((Long) Objects.requireNonNull(Util.getPrimaryKeyValue(values)) == 0) {
                handleInsert(tableName, values, entity);
            } else {
                handleUpdate(tableName, values);
            }
        } catch (PersistenceException e) {
            throw new PersistenceException(e.getMessage());
        }
    }

    @Override
    public void delete(Object entity) {
        Class<?> cls = entity instanceof Proxy ? ((LazyFetchingHandler) Proxy.getInvocationHandler(entity)).getTarget()
                : entity.getClass();
        String tableName = Util.getTableName(cls);
        Long id;
        try {
            // id = getObjectIdValue(entity);
            id = entity instanceof Proxy ? ((LazyFetchingHandler) Proxy.getInvocationHandler(entity)).getTargetId()
                    : getObjectIdValue(entity);
            if (id == null || id == 0) {
//                throw new PersistenceException("Object with class type '" + getClassNameWithoutPackage(entity.getClass())
//                        + "' has not set up 'id' field. Not proceeding with deleting.");
                throw new PersistenceException("Bad id.");
            }
        } catch (IllegalAccessException e) {
//            throw new PersistenceException("Error occurred when accessing 'id' field of an object with class type '"
//                    + getClassNameWithoutPackage(entity.getClass()));
            throw new PersistenceException("Access error.");
        }

        String query = "DELETE FROM [" + tableName + "] WHERE [id] = " + id;
        try {
            Statement statement = connection.createStatement();
            statement.execute(query);
        } catch (SQLException e) {
            throw new PersistenceException("Error occurred when deleting raw from a table " + tableName + "': "
                    + e.getMessage());
        }
        LOGGER.info("Raw was successfully deleted from '" + tableName + "' table");
    }

    private void executeScript(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    Statement statement = connection.createStatement();
                    statement.execute(line);
                } catch (SQLException e) {
                    throw new PersistenceException("Error occurred when executing SQL statement: " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("Error occurred when reading input stream: " + e.getMessage(), e);
        }
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
            Column columnAnnotation = f.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String columnName = columnAnnotation.name().isEmpty() ? f.getName() : columnAnnotation.name();
            String value = rs.getString(columnName);
            f.set(obj, Util.convertDataType(f, value, connection));
        }
        return obj;
    }

    private void handleInsert(String className, List<Property> values, Object entity) throws PersistenceException {
        try {
            long id = insert(className, values);
            Field idField = Objects.requireNonNull(Util.getFieldAnnotatedWith(entity, Id.class));
            idField.setAccessible(true);
            idField.setLong(entity, id);
            LOGGER.info("Raw was successfully inserted into '" + className + "' table.");
        } catch (SQLException e) {
            throw new PersistenceException("Error occurred when inserting row into table '" + className + "': " + e.getMessage());
        } catch (NoSuchFieldException e) {
            throw new PersistenceException("Object with type " + className + " doesn't contain 'id' field");
        } catch (IllegalAccessException e) {
            throw new PersistenceException("Error occurred when setting id to newly create object with type " + className);
        } catch (MissedIdException e) {
            throw new PersistenceException("Error occurred when inserting raw into a table " + className +
                    ". Non primitive object that is behave as FK has not set up ID field");
        }
    }

    private void handleUpdate(String className, List<Property> values) throws PersistenceException {
        try {
            update(className, values);
            LOGGER.info("Raw was successfully updated in '" + className + "' table.");
        } catch (SQLException e) {
            throw new PersistenceException("Error occurred when updating row in table '" + className + "': " + e.getMessage());
        } catch (MissedIdException e) {
            throw new PersistenceException("Error occurred when inserting raw into a table " + className +
                    ". Non primitive object that is behave as FK has not set up ID field");
        } catch (NoSuchFieldException e) {
            throw new PersistenceException("Object with type " + className + " doesn't contain 'id' field");
        } catch (IllegalAccessException e) {
            throw new PersistenceException("Error occurred when retrieving 'id' value from object with type " + className);
        }
    }

    private long insert(String tableName, List<Property> data) throws SQLException, MissedIdException, NoSuchFieldException, IllegalAccessException {
        String query = getInsertQuery(tableName, data);

        PreparedStatement statement = connection.prepareStatement(query);
        int i = 1;
        for (Property property: data) {
            if (property.isPrimaryKey()) {
                continue;
            }
            statement.setObject(i++, castToObject(property.value()));
        }
        int affectedRows = statement.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Creating table " + tableName + " failed, no rows affected.");
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

    private void update(String tableName, List<Property> values) throws SQLException, MissedIdException, NoSuchFieldException, IllegalAccessException {
        String query = getUpdateQuery(tableName, values);

        PreparedStatement statement = connection.prepareStatement(query);
        int i = 1;
        for (Property property: values) {
            if (property.isPrimaryKey()) {
                continue;
            }
            statement.setObject(i++, castToObject(property.value()));
        }
        statement.setObject(i, castToObject(Util.getPrimaryKeyValue(values)));

        statement.executeUpdate();
    }

    private List<Property> getValues(Object obj) throws FieldAccessException {
        Class cls = obj.getClass();
        Field[] fields = cls.getDeclaredFields();
        List<Property> values = new ArrayList<>();
        for (Field field: fields) {
            field.setAccessible(true);
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }
            String columnName = columnAnnotation.name().isEmpty() ? field.getName() : columnAnnotation.name();
            try {
                boolean isId = field.getAnnotation(Id.class) != null;
                Property property = new Property(columnName, field.get(obj), isId);
                values.add(property);
            } catch (IllegalAccessException e) {
                throw new FieldAccessException("Cannot access field '" + columnName + "' in class " + cls.getName());
            }
        }
        return values;
    }

    private String getInsertQuery(String tableName, List<Property> data) {
        StringBuilder names = new StringBuilder("(");
        StringBuilder values = new StringBuilder("(");
        for (Property property: data) {
            if (property.isPrimaryKey()) {
                continue;
            }
            names.append("[").append(property.name()).append("]").append(",");
            values.append("?,");
        }
        names = new StringBuilder(names.substring(0, names.length() - 1));
        values = new StringBuilder(values.substring(0, values.length() - 1));
        names.append(")");
        values.append(")");
        return "INSERT INTO [" + tableName + "]" + names + " values" + values + ";";
    }

    private String getUpdateQuery(String tableName, List<Property> data) {
        StringBuilder query = new StringBuilder("UPDATE [" + tableName + "] SET ");

        for (Property property: data) {
            if (property.isPrimaryKey()) {
                continue;
            }
            query.append("[").append(property.name()).append("]").append("=?,");
        }
        query = new StringBuilder(query.substring(0, query.length() - 1));
        query.append(" WHERE [").append(Util.getPrimaryKeyName(data)).append("] = ?;");
        return query.toString();
    }
}
