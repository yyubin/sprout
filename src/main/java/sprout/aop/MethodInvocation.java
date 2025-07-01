package sprout.aop;

import java.lang.reflect.Method;

public interface MethodInvocation extends JoinPoint{
    Method getMethod();        // 실제 호출될 메서드 (리플렉션 Method 객체)
    Object[] getArguments();   // 메서드 호출 시 사용된 인자 배열
    Object proceed() throws Throwable; // 다음 어드바이스 또는 실제 타겟 메서드
}
