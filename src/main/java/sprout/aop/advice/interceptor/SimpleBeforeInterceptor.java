package sprout.aop.advice.interceptor;

import sprout.aop.JoinPoint;
import sprout.aop.MethodInvocation;
import sprout.aop.ProceedingJoinPoint;
import sprout.aop.advice.Advice;
import sprout.aop.internal.JoinPointAdapter;
import sprout.aop.internal.PjpAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class SimpleBeforeInterceptor implements Advice {
    private final Supplier<Object> aspectProvider;
    private final Method adviceMethod;

    public SimpleBeforeInterceptor(Supplier<Object> aspectProvider,
                                   Method adviceMethod) {
        this.aspectProvider = aspectProvider;
        this.adviceMethod  = adviceMethod;
        adviceMethod.setAccessible(true);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object aspect = java.lang.reflect.Modifier.isStatic(adviceMethod.getModifiers())
                ? null
                : aspectProvider.get();
        try {
            if (adviceMethod.getParameterCount() == 0) {
                adviceMethod.invoke(aspect);
            } else {
                JoinPoint jp = new JoinPointAdapter(invocation);
                adviceMethod.invoke(aspect, jp);
            }
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

        // 실제 타겟 호출
        return invocation.proceed();
    }
}
