package sprout.aop;

import sprout.aop.advisor.AdvisorRegistry;
import sprout.context.CtorMeta;

public interface ProxyFactory {
    Object createProxy(Class<?> targetClass,
                       Object target,
                       AdvisorRegistry registry,
                       CtorMeta meta);
}
