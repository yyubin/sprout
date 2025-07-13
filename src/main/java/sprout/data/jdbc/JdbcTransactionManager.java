package sprout.data.jdbc;

import sprout.beans.annotation.Component;
import sprout.data.TransactionManager;
import sprout.data.core.exception.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class JdbcTransactionManager implements TransactionManager {

    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private final DataSource dataSource;

    public JdbcTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() {
        // 현재 스레드에 트랜잭션 커넥션이 바인딩되어 있는지 확인
        Connection connection = connectionHolder.get();
        if (connection != null) {
            return connection; // 트랜잭션 커넥션이 있으면 그것을 반환
        }

        // 트랜잭션 커넥션이 없으면 (논-트랜잭션 컨텍스트), DataSource에서 새로운 커넥션을 얻어 반환
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get connection from DataSource", e);
        }
    }

    @Override
    public void releaseConnection(Connection connection) {
        if (connection != null && connection == connectionHolder.get()) {
            return;
        }

        if (connection != null) {
            try {
                connection.close(); // 풀로 커넥션 반환
            } catch (SQLException e) {
                System.err.println("Failed to close connection: " + e.getMessage());
            }
        }
    }

    @Override
    public void startTransaction() {
        if (connectionHolder.get() != null) {
            throw new IllegalStateException("Transaction already active for this thread.");
        }

        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            connectionHolder.set(connection);
            System.out.println("Transaction started: " + connection);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to start transaction", e);
        }
    }

    @Override
    public void commit() {
        Connection connection = connectionHolder.get();
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException e) {
                throw new DataAccessException("Failed to commit transaction", e);
            } finally {
                closeConnection();
            }
        } else {
            System.err.println("Commit called without active transaction.");
        }
    }

    @Override
    public void rollback() {
        Connection connection = connectionHolder.get();
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new DataAccessException("Failed to rollback transaction", e);
            } finally {
                closeConnection();
            }
        } else {
            System.err.println("Rollback called without active transaction.");
        }
    }

    private void closeConnection() {
        Connection connection = connectionHolder.get();
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                throw new DataAccessException("Failed to close connection", e);
            } finally {
                connectionHolder.remove();
            }
        }
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }
}
