package http.request;

import controller.Controller;
import controller.annotations.DeleteMapping;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;

import java.lang.reflect.Method;

public class RequestHandler {

    private final Controller controller;

    public RequestHandler(Controller controller) {
        this.controller = controller;
    }

    public void handleRequest(String rawRequest) throws Exception {
        HttpRequest<?> httpRequest = HttpRequestParser.parse(rawRequest, Object.class);

        for (Method method: controller.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                GetMapping getMapping = method.getAnnotation(GetMapping.class);
                if (getMapping.path().equals(httpRequest.getPath())) {
                    method.invoke(controller, httpRequest);
                    return;
                }
            }

            if (method.isAnnotationPresent(PostMapping.class)) {
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                if (postMapping.path().equals(httpRequest.getPath())) {
                    method.invoke(controller, httpRequest);
                    return;
                }
            }

            if (method.isAnnotationPresent(PostMapping.class)) {
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                if (postMapping.path().equals(httpRequest.getPath())) {
                    method.invoke(controller, httpRequest);
                    return;
                }
            }

            if (method.isAnnotationPresent(DeleteMapping.class)) {
                DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                if (deleteMapping.path().equals(httpRequest.getPath())) {
                    method.invoke(controller, httpRequest);
                    return;
                }
            }
        }
    }

}
