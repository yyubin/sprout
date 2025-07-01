package sprout.aop;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class MethodSignature implements Signature{
    private final Method method;

    public MethodSignature(Method method) {
        this.method = method;
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public String toLongName() {
        // 메서드의 전체 시그니처를 반환 (예: public java.lang.String com.example.MyService.myMethod(java.lang.String, int))
        return method.toGenericString();
    }

    @Override
    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }

    public Class<?> getDeclaringType() {
        return method.getDeclaringClass();
    }

    @Override
    public String toString() {
        return getReturnType().getSimpleName() + " " +
                getDeclaringType().getSimpleName() + "." + getName() + "(" +
                Arrays.stream(getParameterTypes())
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", ")) +
                ")";
    }
}
