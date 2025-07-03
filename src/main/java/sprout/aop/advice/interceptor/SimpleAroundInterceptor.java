package sprout.aop.advice.interceptor;

import sprout.aop.MethodInvocation;
import sprout.aop.ProceedingJoinPoint;
import sprout.aop.advice.Advice;
import sprout.aop.internal.PjpAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class SimpleAroundInterceptor implements Advice {
    private final Supplier<Object> aspectProvider;
    private final Method adviceMethod;

    public SimpleAroundInterceptor(Supplier<Object> aspectProvider, Method method) {
        this.aspectProvider = aspectProvider;
        this.adviceMethod = method;
        if (adviceMethod.getParameterCount() != 1 || !ProceedingJoinPoint.class.isAssignableFrom(adviceMethod.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Around advice method must have a single parameter of type ProceedingJoinPoint: " + adviceMethod.getName());
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        ProceedingJoinPoint pjp = new PjpAdapter(invocation);
        Object aspect = aspectProvider.get();
        try {
            adviceMethod.setAccessible(true);
            return adviceMethod.invoke(aspect, pjp);
        } catch (InvocationTargetException e) {
            // 어드바이스 메서드 내부에서 발생한 실제 예외를 던짐
            throw e.getTargetException();
        }
    }
}
