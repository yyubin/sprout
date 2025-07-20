package sprout.aop.internal;

import sprout.aop.MethodInvocation;
import sprout.aop.ProceedingJoinPoint;
import sprout.aop.Signature;

import java.lang.reflect.Method;

public class PjpAdapter implements ProceedingJoinPoint {
    private final MethodInvocation invocation;

    public PjpAdapter(MethodInvocation invocation) { this.invocation = invocation; }

    public Object[] getArgs() { return invocation.getArguments(); }

    @Override
    public Method getMethod() {
        return invocation.getMethod();
    }

    public Signature getSignature() { return invocation.getSignature(); }

    @Override
    public Object getTarget() {
        return invocation.getTarget();
    }

    public Object proceed() throws Throwable { return invocation.proceed(); }
}
