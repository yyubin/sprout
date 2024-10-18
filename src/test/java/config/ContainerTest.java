package config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryMemberRepository;
import service.MemberService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ContainerTest {

    private Container container;

    @BeforeEach
    void setUp() throws Exception {
        container = Container.getInstance();
        container.scan("repository");
        container.scan("component");
        container.scan("service");
    }

    @Test
    void testRepositoryInjection() {
        InMemoryMemberRepository memberRepository = container.get(InMemoryMemberRepository.class);
        assertNotNull(memberRepository);
    }

    @Test
    void testServiceInjection() {
        InMemoryMemberRepository memberRepository = container.get(InMemoryMemberRepository.class);
        assertNotNull(memberRepository);
        MemberService memberService = container.get(MemberService.class);
        assertNotNull(memberService);

        assertNotNull(memberService.getAllMembers());
    }
}
