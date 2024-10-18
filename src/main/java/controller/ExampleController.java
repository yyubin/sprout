package controller;

import config.annotations.Controller;
import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import http.request.HttpRequest;

@Controller
public class ExampleController implements ControllerInterface {

    @GetMapping(path = "/example")
    public void getExample(HttpRequest<?> request) {
        System.out.println("GET request handled");
    }

    @PostMapping(path = "/example")
    public void postExample(HttpRequest<?> request) {
        System.out.println("POST request handled");
    }

}
