package sprout.beans.instantiation;

import sprout.beans.BeanDefinition;
import sprout.context.BeanFactory;

import java.lang.reflect.Parameter;
import java.util.List;

/**
 * 단일 빈 의존성을 해결하는 resolver
 */
public class SingleBeanDependencyResolver implements DependencyTypeResolver {

    private final BeanFactory beanFactory;

    public SingleBeanDependencyResolver(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public boolean supports(Class<?> type) {
        // List 타입이 아닌 모든 타입을 지원
        return !List.class.isAssignableFrom(type);
    }

    @Override
    public Object resolve(Class<?> type, Parameter param, BeanDefinition targetDef) {
        return beanFactory.getBean(type);
    }
}
