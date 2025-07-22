package app.test;

import sprout.beans.annotation.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class TestUserRepository {
    private Map<Long, TestUser> users = new HashMap<>();
    private long id = 0;

    public void saveUser(TestUser user) {
        users.put(id++, user);
    }

    public TestUser getUser(long id) {
        return users.get(id);
    }

}
