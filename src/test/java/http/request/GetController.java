package http.request;

import controller.ControllerInterface;
import controller.annotations.GetMapping;

public class GetController implements ControllerInterface {
    @GetMapping(path = "/testPath")
    public void getMethodWithoutParams() {
        // Method logic
    }
}
