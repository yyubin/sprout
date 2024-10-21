package http.request;

import controller.ControllerInterface;
import controller.annotations.PostMapping;

public class PostController implements ControllerInterface {
    @PostMapping(path = "/postPath")
    public void postMethodWithHttpRequest(HttpRequest<?> request) {
        // Method logic
    }
}
