package sprout.aop;

import java.lang.reflect.Method;

public interface JoinPoint {
    Signature getSignature();
    Object getTarget();
    Object[] getArgs();
    Method getMethod();
}
