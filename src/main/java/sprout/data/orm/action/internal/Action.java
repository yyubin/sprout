package sprout.data.orm.action.internal;

import sprout.data.orm.engine.AfterTransactionCompletionProcess;
import sprout.data.orm.engine.ComparableExecutable;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class Action implements ComparableExecutable, AfterTransactionCompletionProcess {
    abstract void execute(Connection connection) throws SQLException;
}
