package app.test.aop;

import sprout.aop.JoinPoint;
import sprout.aop.ProceedingJoinPoint;
import sprout.aop.annotation.After;
import sprout.aop.annotation.Around;
import sprout.aop.annotation.Aspect;
import sprout.aop.annotation.Before;

@Aspect
public class DemoLoggingAspect {
    @Before(pointcut = "test..*Service.*")
    public void beforeAnyServiceMethod(JoinPoint jp) {
        System.out.println("[BEFORE-SERVICE] " + jp.getSignature().toLongName());
    }

    @Around(pointcut = "test..*Service.save*")
    public Object aroundSave(ProceedingJoinPoint pjp) throws Throwable {
        long t0 = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long elapsed = System.nanoTime() - t0;
            System.out.printf("[AROUND-SAVE] %s took %d µs%n",
                    pjp.getSignature().toLongName(), elapsed / 1_000);
        }
    }

    @After(pointcut = "test..repository.*Repository.find*")
    public void afterFind(JoinPoint jp) {
        System.out.println("[AFTER-FIND] " + jp.getSignature().toLongName());
    }
}
