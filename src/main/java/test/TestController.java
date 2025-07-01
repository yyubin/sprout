package test;

import sprout.beans.annotation.Controller;
import sprout.mvc.annotation.*;

@Controller
@RequestMapping("/api")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/")
    public String testServiceLayerInjection() {
        return testService.test();
    }


    @GetMapping("/test")
    public String test(@RequestParam String id, @RequestParam String name) {
        return "test success : " + id + ", " + name;
    }

    @GetMapping("/test/{id}")
    public String testWithPathVariable(@PathVariable String id) {
        System.out.println("test with path variable : " + id);
        return "test success : " + id;
    }

    @GetMapping("/test/{id}/{name}")
    public String testWithPathVariableAndQueryParam(@PathVariable String id, @PathVariable String name) {
        return "test with path variable and query param : " + id + ", " + name;
    }

    @PostMapping("/test")
    public String testWithPost(@RequestBody TestDto testDto) {
        return testDto.toString();
    }

    @GetMapping("/auth")
    public String authCheck() {
        return testService.authCheck();
    }

}
