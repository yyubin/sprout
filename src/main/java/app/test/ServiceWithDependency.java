package app.test;

import sprout.beans.annotation.Service;

@Service
public class ServiceWithDependency {
    private final SomeService someService;

    public ServiceWithDependency(SomeService someService) {
        this.someService = someService;
    }

    public String callService() {
        return someService.doSomething();
    }
}