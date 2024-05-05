import java.sql.Connection;
import java.sql.SQLException;

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
                throw t;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in transaction", e);
        }
    }
}