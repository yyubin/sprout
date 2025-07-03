package test;

import sprout.beans.annotation.Service;

@Service
public class TestUserService {

    private final TestUserRepository userRepository;

    public TestUserService(TestUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void saveUser(long id, String name) {
        userRepository.saveUser(new TestUser(id, name));
    }

    public TestUser getUser(long id) {
        return userRepository.getUser(id);
    }
}
