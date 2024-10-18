package http.request;

import config.Constants;
import controller.ControllerInterface;
import controller.annotations.DeleteMapping;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import controller.annotations.PutMapping;
import exception.NoMatchingHandlerException;
import exception.UnsupportedHttpMethod;
import message.ExceptionMessage;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class RequestHandler {

    private final ControllerInterface controller;

    public RequestHandler(ControllerInterface controller) {
        this.controller = controller;
    }

    public void handleRequest(String rawRequest) throws Exception, UnsupportedHttpMethod {
        HttpRequest<?> httpRequest = HttpRequestParser.parse(rawRequest, Object.class);

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
        for (Method method : controller.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(mappingClass)) {
                try {
                    String path = (String) mappingClass.getMethod(Constants.path.getConstantsName()).invoke(method.getAnnotation(mappingClass));
                    if (path.equals(httpRequest.getPath())) {
                        method.invoke(controller, httpRequest);
                        return;
                    }
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    throw new NoMatchingHandlerException(ExceptionMessage.NO_MATCHING_PATH, e);
                }
            }
        }
        throw new NoMatchingHandlerException(ExceptionMessage.NO_MATCHING_PATH);
    }

}
