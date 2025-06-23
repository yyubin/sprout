package legacy.request;

import sprout.mvc.mapping.ControllerInterface;
import sprout.mvc.annotation.PostMapping;

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
