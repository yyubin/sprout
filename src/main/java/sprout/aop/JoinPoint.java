package sprout.aop;

public interface JoinPoint {
    Signature getSignature();
    Object getTarget();
    Object[] getArgs();
}
