package test;

import sprout.beans.annotation.Service;
import test.aop.Auth;

@Service
public class TestService {

    public String test() {
        return "TestService Injection Success";
    }

    @Auth
    public String authCheck() {
        return "Auth Check Success";
    }

}
