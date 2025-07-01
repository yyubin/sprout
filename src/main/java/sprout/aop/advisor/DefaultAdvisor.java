package sprout.aop.advisor;

import sprout.aop.advice.Advice;

public class DefaultAdvisor implements Advisor{
    private final Pointcut pointcut;
    private final Advice advice;
    private final int order;

    public DefaultAdvisor(Pointcut pointcut, Advice advice, int order) {
        this.pointcut = pointcut;
        this.advice = advice;
        this.order = order;
    }

    @Override
    public Pointcut getPointcut() { return pointcut; }

    @Override
    public Advice getAdvice() { return advice; }

    @Override
    public int getOrder() { return order; }
}
