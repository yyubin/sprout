package sprout.data;

import javax.sql.DataSource;
import java.sql.Connection;

public interface TransactionManager {

    Connection getConnection();

    void releaseConnection(Connection connection);

    void startTransaction();

    void commit();

    void rollback();

    DataSource getDataSource();
}
