package sprout.aop;

import net.sf.cglib.proxy.Enhancer;
import sprout.aop.advisor.AdvisorRegistry;
import sprout.beans.InfrastructureBean;
import sprout.beans.annotation.Component;
import sprout.context.CtorMeta;

@Component
public class CglibProxyFactory implements ProxyFactory, InfrastructureBean {
    @Override
    public Object createProxy(Class<?> targetClass, Object target, AdvisorRegistry registry, CtorMeta meta) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);
        enhancer.setCallback(new BeanMethodInterceptor(target, registry));
        return enhancer.create(meta.paramTypes(), meta.args());
    }
}
