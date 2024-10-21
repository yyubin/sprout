package service;

import com.sun.tools.javac.Main;
import config.Container;
import config.PackageName;
import domain.Member;
import dto.MemberLoginDTO;
import dto.MemberRegisterDTO;
import exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import repository.InMemoryBoardRepository;
import repository.InMemoryMemberRepository;
import util.RedisSessionManager;
import util.Session;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class MemberAuthServiceTests {

    private MemberAuthService memberAuthService;
    private MemberService memberService;

    @BeforeEach
    public void setup() throws Exception {
        Container.getInstance().scan(PackageName.repository.getPackageName());
        Container.getInstance().scan(PackageName.util.getPackageName());
        Container.getInstance().scan(PackageName.service.getPackageName());
        memberService = Container.getInstance().get(MemberService.class);
        memberAuthService = Container.getInstance().get(MemberAuthService.class);
    }

    @Test
    public void testSuccessfulLogin() throws Throwable {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        String sessionId = memberAuthService.login(memberLoginDTO);

        assertNotNull(sessionId);
    }

    @Test
    public void testFailedLogin() {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        assertThrows(InvalidCredentialsException.class, () -> {
            memberAuthService.login(new MemberLoginDTO("yubin111", "wrongPassword"));
        });
    }

    @Test
    public void testSuccessfulLogout() throws Throwable {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        String sessionId = memberAuthService.login(memberLoginDTO);
        memberAuthService.logout();
        assertNull(Session.getSessionId());
    }

    @Test
    public void testSessionManagementAfterLogin() throws Throwable {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin13", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
        memberService.registerMember(memberDTO);
        String sessionId = memberAuthService.login(new MemberLoginDTO("yubin13", "qwer"));

        assertNotNull(sessionId);

        memberAuthService.logout();
        assertNull(memberAuthService.getRedisSessionManager().getSession(sessionId));
    }

}

