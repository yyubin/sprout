package http.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.Constants;
import config.annotations.Component;
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
public class RequestHandler {

    private List<ControllerInterface> controllers;

    public RequestHandler() {
    }

    public void setControllers(List<ControllerInterface> controllers) {
        this.controllers = controllers;
    }

    public void handleRequest(String rawRequest) throws Exception, UnsupportedHttpMethod {
        HttpRequest<?> httpRequest = HttpRequestParser.parse(rawRequest);
        System.out.println(httpRequest.getBody());
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
            System.out.println(parameterType);
            if (parameterType.equals(String.class)) {
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
        if (method.getParameterCount() == 0) {
            method.invoke(controller);
        } else {
            method.invoke(controller, httpRequest);
        }
    }

}
