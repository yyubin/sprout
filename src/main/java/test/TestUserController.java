package test;

import sprout.beans.annotation.Controller;
import sprout.mvc.annotation.*;
import sprout.mvc.http.ResponseEntity;

@Controller
@RequestMapping("/api/user")
public class TestUserController {

    private final TestUserService testUserService;

    public TestUserController(TestUserService testUserService) {
        this.testUserService = testUserService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable long id) {
        return ResponseEntity.ok(testUserService.getUser(id));
    }

    @PostMapping
    public String saveUser(@RequestBody TestUserDto user) {
        testUserService.saveUser(user.getId(), user.getName());
        return "success";
    }
}
