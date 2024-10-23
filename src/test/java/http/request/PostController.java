package http.request;

import controller.ControllerInterface;
import controller.annotations.PostMapping;

public class PostController implements ControllerInterface {
    @PostMapping(path = "/postPath")
    public void postMethodWithString(AnyModel anyModel) {
        // Method logic
    }

    @PostMapping(path = "/postPathWithoutModel")
    public void postMethodWithoutModel(String key) {
        System.out.println(key);
    }
}
