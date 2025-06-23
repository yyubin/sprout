package legacy.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import app.repository.InMemoryMemberRepository;
import app.service.MemberService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ContainerTest {

    private Container container;

    @BeforeEach
    void setUp() throws Exception {
        container = Container.getInstance();
        container.scan("app/repository");
        container.scan("component");
        container.scan("legacy/service");
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
