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
    private final ThreadLocal<Integer> transactionDepth = new ThreadLocal<>();
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
    public void startTransaction() throws SQLException {
        Integer depth = transactionDepth.get();
        if (depth == null || depth == 0) { // 최상위 트랜잭션 시작
            // 실제 트랜잭션 시작 로직
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            connectionHolder.set(connection);
            transactionDepth.set(1);
            System.out.println("Transaction started (depth 1): " + connection);
        } else { // 중첩된 트랜잭션 진입
            transactionDepth.set(depth + 1);
            System.out.println("Joining existing transaction (depth " + (depth + 1) + ")");
        }
    }

    @Override
    public void commit() {
        Integer depth = transactionDepth.get();
        if (depth == null || depth == 0) {
            System.err.println("Commit called without active transaction.");
            return;
        }

        if (depth == 1) { // 최상위 트랜잭션 커밋
            System.out.println("Committing transaction (depth 1)");
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
        } else {
            transactionDepth.set(depth - 1);
            System.out.println("Leaving nested transaction, commit deferred (depth " + (depth - 1) + ")");
        }

    }

    @Override
    public void rollback() {
        Integer depth = transactionDepth.get();
        if (depth == null || depth == 0) {
            System.err.println("Rollback called without active transaction.");
            return;
        }

        if (depth == 1) { // 최상위 트랜잭션 커밋
            System.out.println("Rollback transaction (depth 1)");
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
        } else {
            transactionDepth.set(depth - 1);
            System.out.println("Leaving nested transaction, rollback deferred (depth " + (depth - 1) + ")");
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
