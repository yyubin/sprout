package sprout.aop.internal;

import sprout.aop.JoinPoint;
import sprout.aop.MethodInvocation;
import sprout.aop.Signature;

public class JoinPointAdapter implements JoinPoint {

    private final MethodInvocation invocation;

    public JoinPointAdapter(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public Object[] getArgs()         { return invocation.getArguments(); }

    @Override
    public Signature getSignature()   { return invocation.getSignature(); }

    @Override
    public Object getTarget()         { return invocation.getTarget(); }
}