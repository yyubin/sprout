package controller;

import controller.annotations.GetMapping;
import controller.annotations.PostMapping;
import http.request.HttpRequest;

@config.annotations.Controller
public class ExampleController implements Controller {

    @GetMapping(path = "/example")
    public void getExample(HttpRequest<?> request) {
        System.out.println("GET request handled");
    }

    @PostMapping(path = "/example")
    public void postExample(HttpRequest<?> request) {
        System.out.println("POST request handled");
    }

}
