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

public class SimpleAfterInterceptor implements Advice {

    private final Supplier<Object> aspectProvider;
    private final Method adviceMethod;

    public SimpleAfterInterceptor(Supplier<Object> aspectProvider,
                                  Method adviceMethod) {
        this.aspectProvider = aspectProvider;
        this.adviceMethod = adviceMethod;
        adviceMethod.setAccessible(true);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        Object result;
        Throwable thrown = null;

        try {
            result = invocation.proceed();
        } catch (Throwable t) {
            thrown = t;
            result = null;          // 필요 시 after-throwing 처리용
        }

        Object aspect = aspectProvider.get();
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

        if (thrown != null) throw thrown;
        return result;
    }
}
