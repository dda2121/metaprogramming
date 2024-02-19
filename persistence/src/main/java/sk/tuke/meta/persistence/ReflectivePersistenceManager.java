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
            System.out.println(query);
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
        String columns = "(";
        for (Field f: fields) {
            f.setAccessible(true);
            columns += f.getName() + " ";
            columns += typeToSQL(f.getType());
            if (f.getName().equals("id")) {
                columns += " PRIMARY KEY,";
            } else {
                columns += ",";
            }
        }
        if (columns.endsWith(",")) {
            columns = columns.substring(0, columns.length() - 1);
        }
        columns += ")";
        return columns;
    }

    private String typeToSQL(Class c) {
        if (c.equals(long.class)) {
            return "INT";
        } else if (c.equals(int.class)) {
            return "INT";
        } else if (c.equals(String.class)) {
            return "VARCHAR(255)";
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
