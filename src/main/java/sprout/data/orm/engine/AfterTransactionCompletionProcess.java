package sprout.data.orm.engine;

public interface AfterTransactionCompletionProcess {
    void afterCompletion(boolean success);
}
