package test;

import sprout.beans.annotation.Service;

@Service
public class TestService {

    public String test() {
        return "TestService Injection Success";
    }

}
