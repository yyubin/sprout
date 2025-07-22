package sprout.aop.advisor;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class RegexPointcut implements Pointcut {
    private final Pointcut delegate;

    public RegexPointcut(String classExpr, String methodExpr) {
        // classExpr 또는 methodExpr 가 비어있을 수 있으니 기본값 처리
        String cls = (classExpr == null || classExpr.isBlank()) ? "..*" : classExpr.trim();
        String mtd = (methodExpr == null || methodExpr.isBlank()) ? "*"   : methodExpr.trim();

        // AspectJ execution 표현식으로 변환
        String aspectj = "execution(* " + cls + "." + mtd + "(..))";
        this.delegate = new AspectJPointcutAdapter(aspectj);
    }

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        return delegate.matches(targetClass, method);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
