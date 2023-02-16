package sk.tuke.meta.persistence;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ReflectivePersistenceManager implements PersistenceManager {

    public ReflectivePersistenceManager(Connection connection, Class<?>... types) {
    }

    @Override
    public void createTables() {
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
    public <T> List<T> getBy(Class<T> type, String fieldName, Object value) {
        return Collections.emptyList();
    }

    @Override
    public long save(Object entity) {
        return 0;
    }

    @Override
    public void delete(Object entity) {
    }
}
