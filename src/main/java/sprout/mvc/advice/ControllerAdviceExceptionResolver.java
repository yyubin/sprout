package sprout.mvc.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.beans.annotation.Component;
import sprout.beans.annotation.Order;
import sprout.mvc.exception.ExceptionResolver;
import sprout.mvc.http.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

@Component
@Order(0)
public class ControllerAdviceExceptionResolver implements ExceptionResolver {
    private final ControllerAdviceRegistry controllerAdviceRegistry;
    private final List<ResponseResolver> responseResolvers;
    private final ObjectMapper objectMapper;

    public ControllerAdviceExceptionResolver(ControllerAdviceRegistry controllerAdviceRegistry, List<ResponseResolver> responseResolvers, ObjectMapper objectMapper) {
        this.controllerAdviceRegistry = controllerAdviceRegistry;
        this.responseResolvers = responseResolvers;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object resolveException(HttpRequest<?> request, HttpResponse response, Object handlerMethod, Exception exception) {
        Optional<ExceptionHandlerObject> handlerOptional = controllerAdviceRegistry.getExceptionHandler(exception.getClass());

        if (handlerOptional.isPresent()) {
            ExceptionHandlerObject exceptionHandler = handlerOptional.get();
            try {
                Method handlerMethodRef = exceptionHandler.getMethod();
                Object handlerInstance = exceptionHandler.getBean();
                Object handlerReturnValue;

                // 예외 핸들러 메서드 시그니처 분석 및 호출
                int paramCount = handlerMethodRef.getParameterCount();
                Class<?>[] paramTypes = handlerMethodRef.getParameterTypes();

                if (paramCount == 0) {
                    handlerReturnValue = handlerMethodRef.invoke(handlerInstance);
                } else if (paramCount == 1 && paramTypes[0].isAssignableFrom(exception.getClass())) {
                    handlerReturnValue = handlerMethodRef.invoke(handlerInstance, exception);
                } else if (paramCount == 2) {
                    if (paramTypes[0].isAssignableFrom(exception.getClass()) && paramTypes[1].isAssignableFrom(HttpRequest.class)) {
                        handlerReturnValue = handlerMethodRef.invoke(handlerInstance, exception, request);
                    } else if (paramTypes[0].isAssignableFrom(HttpRequest.class) && paramTypes[1].isAssignableFrom(exception.getClass())) {
                        handlerReturnValue = handlerMethodRef.invoke(handlerInstance, request, exception);
                    }  else {
                        System.err.println("Unsupported @ExceptionHandler method signature: " + handlerMethodRef.getName());
                        return null; // 처리 못 함
                    }
                } else {
                    System.err.println("Unsupported @ExceptionHandler method signature: " + handlerMethodRef.getName());
                    return null; // 처리 못 함
                }

                // 예외 핸들러의 반환 값을 ResponseResolver를 통해 처리
                for (ResponseResolver resolver : responseResolvers) {
                    if (resolver.supports(handlerReturnValue)) {
                        ResponseEntity<?> responseEntity = resolver.resolve(handlerReturnValue, request);
                        response.setResponseEntity(responseEntity);
                        return new Object(); // 처리 완료를 의미하는 임의의 객체 반환 (null이 아님)
                    }
                }
                System.err.println("No suitable ResponseResolver found for @ExceptionHandler return value: " + handlerReturnValue);
                return null; // 처리 못 함

            } catch (InvocationTargetException ie) {
                System.err.println("Exception in @ExceptionHandler itself: " + ie.getTargetException().getMessage());
                ie.getTargetException().printStackTrace();
            } catch (IllegalAccessException | IllegalArgumentException iae) {
                System.err.println("Error invoking @ExceptionHandler: " + iae.getMessage());
                iae.printStackTrace();
            }
        }
        return null; // 이 ExceptionResolver가 예외를 처리하지 못했음을 알림
    }
}
