package app.controller;

import sprout.beans.annotation.Controller;
import sprout.mvc.annotation.GetMapping;
import sprout.mvc.annotation.PostMapping;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.mapping.ControllerInterface;

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
