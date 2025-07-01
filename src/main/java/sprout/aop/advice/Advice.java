package sprout.aop.advice;

import sprout.aop.MethodInvocation;

public interface Advice {
    // ProceedingJoinPoint를 인자로 받아 어드바이스 로직을 실행
    Object invoke(MethodInvocation invocation) throws Throwable;
}