package sk.tuke.meta.persistence;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ReflectivePersistenceManager implements PersistenceManager {

    public ReflectivePersistenceManager(Connection connection) {
    }

    @Override
    public void createTables(Class<?>... types) {
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
