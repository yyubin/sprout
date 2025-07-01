package sprout.aop.internal;

import sprout.aop.MethodInvocation;
import sprout.aop.ProceedingJoinPoint;
import sprout.aop.Signature;

public class PjpAdapter implements ProceedingJoinPoint {
    private final MethodInvocation invocation;

    public PjpAdapter(MethodInvocation invocation) { this.invocation = invocation; }

    public Object[] getArgs() { return invocation.getArguments(); }

    public Signature getSignature() { return invocation.getSignature(); }

    @Override
    public Object getTarget() {
        return invocation.getTarget();
    }

    public Object proceed() throws Throwable { return invocation.proceed(); }
}
