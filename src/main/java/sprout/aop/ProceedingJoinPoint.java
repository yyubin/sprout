package sprout.aop;

public abstract class ProceedingJoinPoint implements JoinPoint{
    public abstract Object proceed() throws Throwable;
}
