package sk.tuke.meta.persistence.aspects;

import org.aspectj.lang.annotation.Aspect;

import java.sql.Connection;
import java.sql.SQLException;

import sk.tuke.meta.persistence.PersistenceException;
import sk.tuke.meta.persistence.annotations.AtomicPersistenceOperation;
import sk.tuke.meta.persistence.PersistenceManager;

@Aspect
public aspect TransactionAspect {

    private Connection connection;

    pointcut persistenceManagerCreation(Connection connection):
            call(PersistenceManager+.new(..)) && args(connection);

    Object around(Connection connection): persistenceManagerCreation(connection) {
        this.connection = connection;
        return proceed(connection);
    }

    pointcut atomicPersistenceOperation():
            call(@AtomicPersistenceOperation * *(..));

    void around(): atomicPersistenceOperation() {
        try {
            connection.setAutoCommit(false);
            try {
                proceed();
                connection.commit();
            } catch (Throwable t) {
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error in transaction", e);
        }
    }
}