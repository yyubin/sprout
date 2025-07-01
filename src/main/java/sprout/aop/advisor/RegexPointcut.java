package sprout.aop.advisor;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class RegexPointcut implements Pointcut {
    private final Pattern classPattern;
    private final Pattern methodPattern;

    public RegexPointcut(String classRegex, String methodRegex) {
        this.classPattern = Pattern.compile(classRegex);
        this.methodPattern = Pattern.compile(methodRegex);
    }

    @Override
    public boolean matches(Class<?> targetClass, Method method) {
        return classPattern.matcher(targetClass.getName()).matches() &&
                methodPattern.matcher(method.getName()).matches();
    }
}
