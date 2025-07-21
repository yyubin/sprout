package sprout.mvc.advice;

import sprout.beans.annotation.Component;
import sprout.context.BeanFactory;
import sprout.context.Container;
import sprout.mvc.advice.annotation.ControllerAdvice;
import sprout.mvc.advice.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ControllerAdviceRegistry {

    private final List<ExceptionHandlerObject> allExceptionHandlers = new ArrayList<>();
    private final Map<Class<? extends Throwable>, Optional<ExceptionHandlerObject>> cachedHandlers = new ConcurrentHashMap<>();


    public ControllerAdviceRegistry() {
    }

    public Optional<ExceptionHandlerObject> getExceptionHandler(Class<? extends Throwable> exceptionClass) {
        return cachedHandlers.computeIfAbsent(exceptionClass, this::lookupBestMatchHandler);
    }

    private Optional<ExceptionHandlerObject> lookupBestMatchHandler(Class<? extends Throwable> exceptionClass) {
        ExceptionHandlerObject bestMatch = null;
        int bestMatchDistance = Integer.MAX_VALUE; // 예외 타입과의 거리 (낮을수록 구체적)

        for (ExceptionHandlerObject handler : allExceptionHandlers) {
            Method handlerMethod = handler.getMethod();
            for (Class<? extends Throwable> handledExceptionType : handlerMethod.getAnnotation(ExceptionHandler.class).value()) {
                if (handledExceptionType.isAssignableFrom(exceptionClass)) {
                    // 예외 계층상 거리 계산 (더 구체적인 핸들러를 찾기 위함)
                    int distance = getExceptionDistance(handledExceptionType, exceptionClass);

                    if (distance < bestMatchDistance) {
                        bestMatch = handler;
                        bestMatchDistance = distance;
                    }
                }
            }
        }
        return Optional.ofNullable(bestMatch);
    }

    private int getExceptionDistance(Class<?> fromClass, Class<?> toClass) {
        if (fromClass.equals(toClass)) {
            return 0; // 정확히 일치
        }
        int distance = 0;
        Class<?> current = toClass;
        while (current != null && !current.equals(fromClass)) {
            current = current.getSuperclass();
            distance++;
        }
        return (current != null) ? distance : Integer.MAX_VALUE; // 찾지 못하면 무한대
    }

    public void scanControllerAdvices(BeanFactory context) {
        Collection<Object> allBeans = context.getAllBeans();
        for (Object bean : allBeans) {
            if (bean.getClass().isAnnotationPresent(ControllerAdvice.class)) {
                System.out.println("Found @ControllerAdvice: " + bean.getClass().getName());
                for (Method method : bean.getClass().getDeclaredMethods()) {
                    if (method.isAnnotationPresent(ExceptionHandler.class)) {
                        method.setAccessible(true);
                        allExceptionHandlers.add(new ExceptionHandlerObject(method, bean));
                        System.out.println("  Registered @ExceptionHandler: " + method.getName() + " for types: " +
                                Arrays.stream(method.getAnnotation(ExceptionHandler.class).value()).map(Class::getSimpleName).collect(Collectors.joining(", ")));
                    }
                }
            }
        }
    }
}
