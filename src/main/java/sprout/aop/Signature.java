package sprout.aop;

public interface Signature {
    String getName();
    String toLongName();
    Class<?> getReturnType();
    Class<?>[] getParameterTypes();
}
