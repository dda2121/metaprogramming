package sk.tuke.meta.persistence;

import sk.tuke.meta.persistence.handler.LazyFetchingHandler;
import sk.tuke.meta.persistence.model.DAO;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class DAOPersistenceManager implements PersistenceManager {

    private final Connection connection;
    private final Map<Class<?>, DAO<?>> daoMap = new HashMap<>();

    protected DAOPersistenceManager(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTables(Class<?>... types) {
        for (DAO<?> dao : daoMap.values()) {
            dao.createTable();
        }
    }

    protected <T> void putDao(Class<T> type, DAO<T> dao) {
        daoMap.put(type, dao);
    }

    @SuppressWarnings("unchecked")
    protected <T> DAO<T> getDao(Class<T> type) {
        return (DAO<T>) daoMap.get(type);
    }

    @Override
    public <T> Optional<T> get(Class<T> type, long id) {
        return getDao(type).get(id);
    }

    @Override
    public <T> List<T> getAll(Class<T> type) {
        return getDao(type).getAll();
    }

    @Override
    public <T> void save(T entity) {
        if (entity instanceof Proxy) {
            LazyFetchingHandler handler = (LazyFetchingHandler) Proxy.getInvocationHandler(entity);
            getDao(handler.getTarget()).save(entity);
        } else {
            getDao(entity.getClass()).save(entity);
        }
    }

    @Override
    public void delete(Object entity) {
        if (entity instanceof Proxy) {
            LazyFetchingHandler handler = (LazyFetchingHandler) Proxy.getInvocationHandler(entity);
            getDao(handler.getTarget()).delete(entity);
        } else {
            getDao(entity.getClass()).delete(entity);
        }
    }
}
