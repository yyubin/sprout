package sprout.beans.processor;

public interface BeanPostProcessor {
    default Object postProcessBeforeInitialization(String beanName, Object bean) {
        return bean;
    }
    default Object postProcessAfterInitialization(String beanName, Object bean) {
        return bean;
    }
}
