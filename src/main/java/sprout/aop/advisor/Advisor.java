package sprout.aop.advisor;


import sprout.aop.advice.Advice;

public interface Advisor {
    Pointcut getPointcut();
    Advice getAdvice();
    default int getOrder() {
        return Integer.MAX_VALUE; // 기본값, 가장 낮은 우선순위
    }
}
