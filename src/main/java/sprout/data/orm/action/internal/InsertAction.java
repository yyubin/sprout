package sprout.data.orm.action.internal;

import sprout.data.orm.action.Executable;
import sprout.data.orm.engine.ComparableExecutable;
import sprout.data.orm.engine.Parameter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class InsertAction extends Action{
    private final String sql; // e.g., "INSERT INTO user (id, name) VALUES (?, ?)"
    private final List<Parameter<?>> parameters;

    public InsertAction(String sql, List<Parameter<?>> parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    public String getSql() {
        return sql;
    }

    public List<Parameter<?>> getParameters() {
        return parameters;
    }

    @Override
    public Type getActionType() {
        return null;
    }

    @Override
    void execute(Connection connection) throws SQLException {

    }

    @Override
    public void afterCompletion(boolean success) {

    }

    @Override
    public String getPrimarySort() {
        return "";
    }

    @Override
    public String getSecondarySort() {
        return "";
    }

    @Override
    public List<ComparableExecutable> getDependencies() {
        return List.of();
    }

    @Override
    public int compareTo(Executable o) {
        return 0;
    }

    @Override
    public void execute() {

    }
}
