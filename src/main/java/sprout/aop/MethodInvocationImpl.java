package sprout.aop;

import net.sf.cglib.proxy.MethodProxy;
import sprout.aop.advisor.Advisor;

import java.lang.reflect.Method;
import java.util.List;

public class MethodInvocationImpl implements MethodInvocation{
    private final Object target;           // 실제 메서드가 호출될 대상 객체
    private final Method method;           // 호출될 메서드의 Method 객체
    private final Object[] args;           // 메서드 호출 인자
    private final MethodProxy methodProxy; // CGLIB의 메서드 프록시 (실제 타겟 메서드 호출용)
    private final List<Advisor> advisors;  // 현재 적용 가능한 어드바이저 목록
    private int currentAdvisorIndex = -1;  // 현재 실행할 어드바이저의 인덱스

    public MethodInvocationImpl(Object target, Method method, Object[] args, MethodProxy methodProxy, List<Advisor> advisors) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.methodProxy = methodProxy;
        this.advisors = advisors;
    }

    @Override
    public Signature getSignature() {
        return new MethodSignature(method); // MethodSignature 객체를 사용하여 시그니처 정보 제공
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Object[] getArgs() {
        return args;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getArguments() {
        return args;
    }

    @Override
    public Object proceed() throws Throwable {
        currentAdvisorIndex++; // 다음 어드바이저로 이동

        if (currentAdvisorIndex < advisors.size()) {
            // 다음 어드바이저의 Advice 실행
            Advisor advisor = advisors.get(currentAdvisorIndex);
            // advisor.getAdvice()는 sprout.aop.advice.Advice 인터페이스를 반환해야 합니다.
            // Advice 인터페이스는 invoke(ProceedingJoinPoint pjp)를 가집니다.
            return advisor.getAdvice().invoke(this);
        } else {
            // 모든 어드바이저를 실행했으면 실제 타겟 메서드 호출
            return methodProxy.invoke(target, args);
        }
    }
}
