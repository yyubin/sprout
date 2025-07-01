package sprout.aop;

public interface ProceedingJoinPoint extends JoinPoint{
    Object proceed() throws Throwable;
}
