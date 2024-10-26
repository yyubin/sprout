package http.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.Constants;
import config.annotations.Component;
import config.annotations.ExceptionHandler;
import config.annotations.Priority;
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
import util.Session;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Priority(value = 2)
@Requires(dependsOn = {ExceptionProcessor.class, ObjectMapperConfig.class, HttpRequestParser.class})
public class RequestHandler {

    private final ExceptionProcessor exceptionResolver;
    private final ObjectMapperConfig objectMapper;
    private final HttpRequestParser httpRequestParser;
    private List<ControllerInterface> controllers;

    public RequestHandler(ExceptionProcessor exceptionResolver, ObjectMapperConfig objectMapper, HttpRequestParser httpRequestParser) {
        this.exceptionResolver = exceptionResolver;
        this.objectMapper = objectMapper;
        this.httpRequestParser = httpRequestParser;
    }

    public void setControllers(List<ControllerInterface> controllers) {
        this.controllers = controllers;
    }

    public void handleRequest(String rawRequest) throws Exception, UnsupportedHttpMethod {
        HttpRequest<?> httpRequest = httpRequestParser.parse(rawRequest);
        handlerUserSession(httpRequest);

        if (httpRequest.getMethod().equals(HttpMethod.GET)) {
            handleRequestByMethod(httpRequest, GetMapping.class);
            return;
        }
        if (httpRequest.getMethod().equals(HttpMethod.POST)) {
            handleRequestByMethod(httpRequest, PostMapping.class);
            return;
        }
        if (httpRequest.getMethod().equals(HttpMethod.PUT)) {
            handleRequestByMethod(httpRequest, PutMapping.class);
            return;
        }
        if (httpRequest.getMethod().equals(HttpMethod.DELETE)) {
            handleRequestByMethod(httpRequest, DeleteMapping.class);
            return;
        }

        throw new UnsupportedHttpMethod(ExceptionMessage.UNSUPPORTED_HTTP_METHOD);
    }

    private void handleRequestByMethod(HttpRequest<?> httpRequest, Class<? extends Annotation> mappingClass) throws Exception {
        boolean result = false;
        for (ControllerInterface controller : controllers) {
            for (Method method : controller.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(mappingClass)) {
                    result = handleRequestMethod(httpRequest, mappingClass, controller, method);
                }
                if (result) {
                    return;
                }
            }
        }
        throw new BadRequestException();
    }

    private boolean handleRequestMethod(HttpRequest<?> httpRequest, Class<? extends Annotation> mappingClass, ControllerInterface controller, Method method) throws Exception {
        String path = (String) mappingClass.getMethod(Constants.path.getConstantsName()).invoke(method.getAnnotation(mappingClass));
        if (path.equals(httpRequest.getPath())) {
            Object[] parameters = resolveParameters(method, (HttpRequest<Map<String, Object>>) httpRequest);
            invokeMethod(controller, method, parameters);
            return true;
        }
        return false;
    }

    private Object[] resolveParameters(Method method, HttpRequest<Map<String, Object>> httpRequest) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        Map<String, Object> body = httpRequest.getBody();

        String[] parameterNames = getParameterNames(method);
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            String paramName = parameterNames[i];

            if (parameterType.equals(Long.class)) {
                String queryParamValue = httpRequest.getQueryParams().get(paramName);
                if (queryParamValue != null) {
                    parameters[i] = Long.valueOf(queryParamValue);
                } else {
                    Long valueFromBody = parseBodyToModel(body, paramName, Long.class);
                    if (valueFromBody != null) {
                        parameters[i] = valueFromBody;
                    }
                }
            } else if (parameterType.equals(String.class)) {
                String queryParamValue = httpRequest.getQueryParams().get(paramName);
                if (queryParamValue != null) {
                    parameters[i] = queryParamValue;
                } else {
                    String valueFromBody = parseBodyToModel(body, paramName, String.class);
                    if (valueFromBody != null) {
                        parameters[i] = valueFromBody;
                    }
                }
            } else {
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

    private <T> T parseBodyToModel(Map<String, Object> body, String paramName, Class<T> modelClass) throws Exception {
        Object value = body.get(paramName);
        if (value == null) {
            return null;
        }

        if (modelClass.equals(String.class)) {
            return modelClass.cast(value.toString());
        }

        if (modelClass.equals(Long.class)) {
            return modelClass.cast(Long.valueOf(value.toString()));
        }

        return null;
    }

    private <T> T parseBodyToModel(Map<String, Object> body, Class<T> modelClass) throws Exception {
        return objectMapper.getObjectMapper().convertValue(body, modelClass);
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
            throw new Exception(result);
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

    private void handlerUserSession(HttpRequest<?> httpRequest) {
        Session.setSessionId(httpRequest.getSessionId());
    }

}
