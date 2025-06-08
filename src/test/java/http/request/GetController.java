package http.request;

import sprout.mvc.mapping.ControllerInterface;
import sprout.mvc.annotation.GetMapping;

public class GetController implements ControllerInterface {
    @GetMapping(path = "/testPath")
    public void getMethodWithoutParams() {
        // Method logic
    }
}
