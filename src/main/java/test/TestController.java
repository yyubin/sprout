package test;

import sprout.beans.annotation.Controller;
import sprout.mvc.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/test")
    public String test() {
        System.out.println("test");
        return "test";
    }
}
