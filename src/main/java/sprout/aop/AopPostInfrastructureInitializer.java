package sprout.aop;

import sprout.beans.annotation.Component;
import sprout.context.BeanFactory;
import sprout.context.PostInfrastructureInitializer;

import java.util.List;

@Component
public class AopPostInfrastructureInitializer implements PostInfrastructureInitializer {
    private final AspectPostProcessor aspectPostProcessor;

    public AopPostInfrastructureInitializer(AspectPostProcessor aspectPostProcessor) {
        this.aspectPostProcessor = aspectPostProcessor;
    }

    @Override
    public void afterInfrastructureSetup(BeanFactory beanFactory, List<String> basePackages) {
        aspectPostProcessor.initialize(basePackages);
    }
}
