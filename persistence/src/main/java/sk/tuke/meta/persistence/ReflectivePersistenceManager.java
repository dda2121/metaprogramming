package sk.tuke.meta.persistence;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ReflectivePersistenceManager implements PersistenceManager {

    private Connection connection;

    public ReflectivePersistenceManager(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTables(Class<?>... types) {
        for (Class c: types) {
            Field[] fields = c.getDeclaredFields();
            String query = "CREATE TABLE IF NOT EXISTS " + getClassNameWithoutPackage(c).toLowerCase()
                    + getTableScript(fields);
            try {
                Statement statement = connection.createStatement();
                statement.execute(query);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getClassNameWithoutPackage(Class<?> c) {
        String fullClassName = c.getName();
        int lastDotIndex = fullClassName.lastIndexOf('.');

        if (lastDotIndex == -1) {
            return fullClassName;
        } else {
            return fullClassName.substring(lastDotIndex + 1);
        }
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
        columns.append(")");
        return columns.toString();
    }

    private String typeToSQL(Class c) {
        if (c.equals(long.class) || c.equals(int.class)) {
            return "INT";
        } else if (c.equals(double.class) || c.equals(float.class)) {
            return "DOUBLE";
        } else if (c.equals(char.class)) {
            return "CHAR";
        } else if (c.equals(String.class)) {
            return "VARCHAR(255)";
        } else if (c.equals(boolean.class)) {
            return "BOOLEAN";
        } else {
            return "INT";
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> type, long id) {
        return Optional.empty();
    }

    @Override
    public <T> List<T> getAll(Class<T> type) {
        return Collections.emptyList();
    }

    @Override
    public void save(Object entity) {
    }

    @Override
    public void delete(Object entity) {
    }
}
