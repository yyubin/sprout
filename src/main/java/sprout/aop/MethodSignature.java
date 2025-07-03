package sprout.aop;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class MethodSignature implements Signature{
    private final Method method;
    private volatile String cachedToString;
    private volatile String cachedLongName;

    public MethodSignature(Method method) {
        this.method = method;
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public String toLongName() {
        String local = cachedLongName;
        if (local == null) {                      // 첫 호출
            synchronized (this) {
                if (cachedLongName == null) {     // 2차 확인
                    cachedLongName = method.toGenericString();
                }
                local = cachedLongName;
            }
        }
        return local;
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
        String local = cachedToString;
        if (local != null) return local;

        synchronized (this) {
            if (cachedToString == null) {
                cachedToString =
                        getReturnType().getSimpleName() + " " +
                                getDeclaringType().getSimpleName() + "." + getName() + "(" +
                                Arrays.stream(getParameterTypes())
                                        .map(Class::getSimpleName)
                                        .collect(Collectors.joining(", ")) +
                                ")";
            }
            return cachedToString;
        }
    }
}
