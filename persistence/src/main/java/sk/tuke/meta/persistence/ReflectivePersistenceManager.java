package sk.tuke.meta.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static sk.tuke.meta.persistence.util.SQLUtil.typeToSQL;
import static sk.tuke.meta.persistence.util.Util.getClassNameWithoutPackage;


public class ReflectivePersistenceManager implements PersistenceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectivePersistenceManager.class.getName());

    private Connection connection;

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
}
