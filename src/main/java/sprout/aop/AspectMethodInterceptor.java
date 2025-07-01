package sprout.aop;

import sprout.aop.advice.Advice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AspectMethodInterceptor implements Advice {
    private final Object aspectInstance; // Aspect 클래스의 인스턴스
    private final Method adviceMethod;

    public AspectMethodInterceptor(Object aspectInstance, Method adviceMethod) {
        this.aspectInstance = aspectInstance;
        this.adviceMethod = adviceMethod;
        if (adviceMethod.getParameterCount() != 1 || !ProceedingJoinPoint.class.isAssignableFrom(adviceMethod.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Around advice method must have a single parameter of type ProceedingJoinPoint: " + adviceMethod.getName());
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            // Aspect의 어드바이스 메서드 호출
            // @Around 메서드는 첫 번째 인자로 MethodInvocation (ProceedingJoinPoint 역할)
            return adviceMethod.invoke(aspectInstance, invocation);
        } catch (InvocationTargetException e) {
            // 어드바이스 메서드 내부에서 발생한 실제 예외를 던짐
            throw e.getTargetException();
        }
    }
}
