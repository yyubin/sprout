package sprout.context;

public interface ContextInitializer {
    void initializeAfterRefresh(BeanFactory context);
}
