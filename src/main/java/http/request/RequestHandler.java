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
                    try {
                        String path = (String) mappingClass.getMethod(Constants.path.getConstantsName()).invoke(method.getAnnotation(mappingClass));
                        if (path.equals(httpRequest.getPath())) {
                            if (method.getParameterCount() == 0) {
                                method.invoke(controller);
                            } else {
                                method.invoke(controller, httpRequest);
                            }
                            return;
                        }
                    } catch (NoSuchMethodException | IllegalAccessException e) {
                        throw new NoMatchingHandlerException(ExceptionMessage.NO_MATCHING_PATH, e);
                    }
                }
            }
        }
        throw new NoMatchingHandlerException(ExceptionMessage.NO_MATCHING_PATH);
    }

}
