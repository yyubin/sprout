package sprout.data.transaction.aop;

import sprout.aop.ProceedingJoinPoint;
import sprout.aop.annotation.Around;
import sprout.aop.annotation.Aspect;
import sprout.data.TransactionManager;
import sprout.data.transaction.annotation.SproutTransactional;

@Aspect
public class TransactionalAspect {

    private final TransactionManager transactionManager;

    public TransactionalAspect(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Around(annotation = SproutTransactional.class)
    public Object manageTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        transactionManager.startTransaction();
        try {
            Object result = joinPoint.proceed();
            transactionManager.commit();
            return result;
        } catch (Exception e) {
            transactionManager.rollback();
            throw e;
        }
    }
}
