package sprout.aop.advisor;

import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;

import java.lang.reflect.Method;
import java.util.EnumSet;

public final class AspectJPointcutAdapter implements Pointcut {

    private static final PointcutParser PARSER =
            PointcutParser.getPointcutParserSupportingAllPrimitivesAndUsingContextClassloaderForResolution();


    private final PointcutExpression expression;

    public AspectJPointcutAdapter(String expr) {
        this.expression = PARSER.parsePointcutExpression(expr);
    }

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        // 클래스/메서드 둘 다 체크 (alwaysMatches / maybeMatches 구분)
        var sm = expression.matchesMethodExecution(method);
        return sm.alwaysMatches() || sm.maybeMatches();
    }

    @Override
    public String toString() {
        return "AspectJPointcutAdapter{" + expression.getPointcutExpression() + '}';
    }
}
