package sprout.mvc.advice;

import sprout.beans.annotation.Component;
import sprout.context.BeanFactory;
import sprout.context.ContextInitializer;

@Component
public class ControllerAdviceContextInitializer implements ContextInitializer {

    private final ControllerAdviceRegistry registry;

    public ControllerAdviceContextInitializer(ControllerAdviceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void initializeAfterRefresh(BeanFactory context) {
        registry.scanControllerAdvices(context);
    }
}
