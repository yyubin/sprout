package sprout.data;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public interface TransactionManager {

    Connection getConnection();

    void releaseConnection(Connection connection);

    void startTransaction() throws SQLException;

    void commit();

    void rollback();

    DataSource getDataSource();
}
