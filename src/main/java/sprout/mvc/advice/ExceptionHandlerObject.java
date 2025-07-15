package sprout.mvc.advice;

import java.lang.reflect.Method;

public class ExceptionHandlerObject {
    private final Method method;
    private final Object bean;

    public ExceptionHandlerObject(Method method, Object bean) {
        this.method = method;
        this.bean = bean;
        method.setAccessible(true);
    }

    public Method getMethod() {
        return method;
    }

    public Object getBean() {
        return bean;
    }
}
