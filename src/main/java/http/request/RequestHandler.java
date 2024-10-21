package http.request;

import config.Constants;
import config.annotations.Component;
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
import java.util.List;

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
        for (ControllerInterface controller : controllers) {
            for (Method method : controller.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(mappingClass)) {
                    handleRequestMethod(httpRequest, mappingClass, controller, method);
                    return;
                }
            }
        }
        throw new NoMatchingHandlerException(ExceptionMessage.NO_MATCHING_PATH);
    }

    private void handleRequestMethod(HttpRequest<?> httpRequest, Class<? extends Annotation> mappingClass, ControllerInterface controller, Method method) throws Exception {
        String path = (String) mappingClass.getMethod(Constants.path.getConstantsName()).invoke(method.getAnnotation(mappingClass));
        if (path.equals(httpRequest.getPath())) {
            invokeMethod(controller, method, httpRequest);
        }
    }

    private void invokeMethod(ControllerInterface controller, Method method, HttpRequest<?> httpRequest) throws Exception {
        if (method.getParameterCount() == 0) {
            method.invoke(controller);
        } else {
            method.invoke(controller, httpRequest);
        }
    }

}
