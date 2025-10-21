package sprout.beans.instantiation;

import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.context.BeanFactory;

import java.lang.reflect.Method;

/**
 * 팩토리 메서드 기반 빈 인스턴스화 전략
 */
public class FactoryMethodBasedInstantiationStrategy implements BeanInstantiationStrategy {

    @Override
    public Object instantiate(BeanDefinition def, DependencyResolver dependencyResolver, BeanFactory beanFactory) throws Exception {
        // 팩토리 빈 조회
        Object factoryBean = beanFactory.getBean(def.getFactoryBeanName());
        Method factoryMethod = def.getFactoryMethod();

        // 팩토리 메서드의 의존성 해결
        Object[] deps = dependencyResolver.resolve(
                def.getFactoryMethodArgumentTypes(),
                factoryMethod.getParameters(),
                def
        );

        // 팩토리 메서드 호출 (접근 권한 설정)
        factoryMethod.setAccessible(true);
        return factoryMethod.invoke(factoryBean, deps);
    }

    @Override
    public boolean supports(BeanCreationMethod method) {
        return method == BeanCreationMethod.FACTORY_METHOD;
    }
}
