package app.test.aop;

import sprout.aop.ProceedingJoinPoint;
import sprout.aop.annotation.Around;
import sprout.aop.annotation.Aspect;

@Aspect
public class TestAspect {
    @Around(annotation = {Auth.class})
    public Object authCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Auth Check");
        return joinPoint.proceed();
    }

}
