package sprout.beans.instantiation;

import net.sf.cglib.proxy.Enhancer;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.context.BeanFactory;
import sprout.context.ConfigurationMethodInterceptor;

import java.lang.reflect.Constructor;

/**
 * 생성자 기반 빈 인스턴스화 전략
 */
public class ConstructorBasedInstantiationStrategy implements BeanInstantiationStrategy {

    @Override
    public Object instantiate(BeanDefinition def, DependencyResolver dependencyResolver, BeanFactory beanFactory) throws Exception {
        Constructor<?> constructor = def.getConstructor();
        Object[] deps;

        // ConstructorBeanDefinition인 경우 미리 준비된 생성자 인자 사용
        if (def instanceof ConstructorBeanDefinition && ((ConstructorBeanDefinition) def).getConstructorArguments() != null) {
            deps = ((ConstructorBeanDefinition) def).getConstructorArguments();
        } else {
            // 의존성 해결
            deps = dependencyResolver.resolve(
                    def.getConstructorArgumentTypes(),
                    constructor.getParameters(),
                    def
            );
        }

        // Configuration 클래스의 경우 CGLIB 프록시 생성
        if (def.isConfigurationClassProxyNeeded()) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(def.getType());
            enhancer.setCallback(new ConfigurationMethodInterceptor(beanFactory));
            return enhancer.create(def.getConstructorArgumentTypes(), deps);
        } else {
            // 접근 권한 설정 (private/package-private 생성자 접근 가능하게)
            constructor.setAccessible(true);
            return constructor.newInstance(deps);
        }
    }

    @Override
    public boolean supports(BeanCreationMethod method) {
        return method == BeanCreationMethod.CONSTRUCTOR;
    }
}
