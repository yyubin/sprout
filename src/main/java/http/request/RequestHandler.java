package http.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.Constants;
import config.annotations.Component;
import config.annotations.ExceptionHandler;
import config.annotations.Requires;
import config.exception.ExceptionProcessor;
import config.exception.ExceptionResolver;
import controller.ControllerInterface;
import controller.annotations.DeleteMapping;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import controller.annotations.PutMapping;
import exception.BadRequestException;
import exception.NoMatchingHandlerException;
import exception.UnsupportedHttpMethod;
import message.ExceptionMessage;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Requires(dependsOn = {ExceptionProcessor.class})
public class RequestHandler {

    private final ExceptionProcessor exceptionResolver;
    private List<ControllerInterface> controllers;

    public RequestHandler(ExceptionProcessor exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    public void setControllers(List<ControllerInterface> controllers) {
        this.controllers = controllers;
    }

    public String handleRequest(String rawRequest) throws Exception, UnsupportedHttpMethod {
        HttpRequest<?> httpRequest = HttpRequestParser.parse(rawRequest);
        if (httpRequest.getMethod().equals(HttpMethod.GET)) {
            return handleRequestByMethod(httpRequest, GetMapping.class);
        }
        if (httpRequest.getMethod().equals(HttpMethod.POST)) {
            return handleRequestByMethod(httpRequest, PostMapping.class);
        }
        if (httpRequest.getMethod().equals(HttpMethod.PUT)) {
            return handleRequestByMethod(httpRequest, PutMapping.class);
        }
        if (httpRequest.getMethod().equals(HttpMethod.DELETE)) {
            return handleRequestByMethod(httpRequest, DeleteMapping.class);
        }

        throw new UnsupportedHttpMethod(ExceptionMessage.UNSUPPORTED_HTTP_METHOD);
    }

    private String handleRequestByMethod(HttpRequest<?> httpRequest, Class<? extends Annotation> mappingClass) throws Exception {
        boolean result = false;
        for (ControllerInterface controller : controllers) {
            for (Method method : controller.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(mappingClass)) {
                    result = handleRequestMethod(httpRequest, mappingClass, controller, method);
                }
                if (result) {
                    return null;
                }
            }
        }
        throw new BadRequestException();
    }

    private boolean handleRequestMethod(HttpRequest<?> httpRequest, Class<? extends Annotation> mappingClass, ControllerInterface controller, Method method) throws Exception {
        String path = (String) mappingClass.getMethod(Constants.path.getConstantsName()).invoke(method.getAnnotation(mappingClass));
        if (path.equals(httpRequest.getPath())) {
            Object[] parameters = resolveParameters(method, httpRequest);
            invokeMethod(controller, method, parameters);
            return true;
        }
        return false;
    }

    private Object[] resolveParameters(Method method, HttpRequest<?> httpRequest) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];

        String[] parameterNames = getParameterNames(method);
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            String paramName = parameterNames[i];
            if (parameterType.equals(Long.class)) {
                String queryParamValue = httpRequest.getQueryParams().get(paramName);
                if (queryParamValue != null) {
                    parameters[i] = Long.valueOf(queryParamValue);
                }
            } else if (parameterType.equals(String.class)) {
                parameters[i] = httpRequest.getQueryParams().get(paramName);
            } else {
                String body = (String) httpRequest.getBody();
                try {
                    parameters[i] = parseBodyToModel(body, parameterType);
                } catch (Exception e) {
                    throw new Exception(e);
                }
            }
        }
        return parameters;
    }

    private String[] getParameterNames(Method method) {
        // Java 8 이상에서 -parameters 플래그를 사용해야 함
        return Arrays.stream(method.getParameters())
                .map(Parameter::getName)
                .toArray(String[]::new);
    }

    private <T> T parseBodyToModel(String body, Class<T> modelClass) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(body, modelClass);
    }


    private void invokeMethod(ControllerInterface controller, Method method, Object[] httpRequest) throws Exception {
        try {
            if (method.getParameterCount() == 0) {
                method.invoke(controller);
            } else {
                method.invoke(controller, httpRequest);
            }
        } catch (Throwable e) {
            String result = handlerException(e);
            throw new Exception(e);
        }

    }

    private String handlerException(Throwable e) throws Exception {
        for (Method method : exceptionResolver.getClass().getMethods()) {
            if (method.isAnnotationPresent(ExceptionHandler.class)) {
                ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);

                if (annotation.disposeOf().isAssignableFrom(e.getClass())) {
                    try {
                        return (String) method.invoke(exceptionResolver, e);
                    } catch (Exception ex) {
                        return exceptionResolver.handleUndefinedException(ex);
                    }
                }
            }
        }
        return exceptionResolver.handleUndefinedException((Exception) e);
    }

}
