package sk.tuke.meta.persistence.model;

import java.util.List;
import java.util.Optional;

public interface DAO<T> {
    void createTable();

    Optional<T> get(long id);

    List<T> getAll();

    void save(Object entity);

    void delete(Object entity);
}
