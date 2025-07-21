package sprout.mvc.mapping;

import sprout.beans.annotation.Component;
import sprout.context.BeanFactory;
import sprout.context.ContextInitializer;

@Component
public class HandlerContextInitializer implements ContextInitializer {
    private final HandlerMethodScanner scanner;

    public HandlerContextInitializer(HandlerMethodScanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public void initializeAfterRefresh(BeanFactory context) {
        scanner.scanControllers(context);
    }
}
