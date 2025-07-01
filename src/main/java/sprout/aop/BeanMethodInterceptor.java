package sprout.aop;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.AdvisorRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class BeanMethodInterceptor implements MethodInterceptor {

    private final Object target; // Aspect 클래스의 인스턴스
    private final AdvisorRegistry advisorRegistry;

    public BeanMethodInterceptor(Object target, AdvisorRegistry advisorRegistry) {
        this.target = target;
        this.advisorRegistry = advisorRegistry;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        List<Advisor> applicableAdvisors = advisorRegistry.getApplicableAdvisors(target.getClass(), method);

        if (applicableAdvisors.isEmpty()) {
            // 적용할 Advisor가 없으면 원본 메서드 호출
            return proxy.invoke(target, args);
        }

        // MethodInvocationImpl을 사용하여 Advice 체인 실행
        MethodInvocationImpl invocation = new MethodInvocationImpl(target, method, args, proxy, applicableAdvisors);
        return invocation.proceed();
    }
}
