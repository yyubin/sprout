package sprout.aop.advisor;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class RegexPointcut implements Pointcut {
    private final Pattern classPattern;
    private final Pattern methodPattern;

    public RegexPointcut(String classRegex, String methodRegex) {
        this.classPattern  = Pattern.compile(adapt(classRegex));
        this.methodPattern = Pattern.compile(adapt(methodRegex));
    }

    private static String adapt(String expr) {
        String regex = AspectJRegexConverter.toRegex(expr.trim());
        return regex.isEmpty() ? ".*" : regex;
    }

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        return classPattern.matcher(targetClass.getName()).find() &&
                methodPattern.matcher(method.getName()).find();
    }
}
