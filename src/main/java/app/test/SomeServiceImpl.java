package app.test;

import sprout.beans.annotation.Service;

@Service
public class SomeServiceImpl implements SomeService {
    @Override
    public String doSomething() {
        return "Done";
    }
}
