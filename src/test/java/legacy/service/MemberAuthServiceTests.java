package legacy.service;

import legacy.config.Container;
import legacy.config.PackageName;
import app.dto.MemberLoginDTO;
import app.dto.MemberRegisterDTO;
import app.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import app.service.interfaces.MemberAuthServiceInterface;
import app.service.interfaces.MemberServiceInterface;
import app.util.Session;

import static org.junit.jupiter.api.Assertions.*;

public class MemberAuthServiceTests {

    private MemberAuthServiceInterface memberAuthService;
    private MemberServiceInterface memberService;

    @BeforeEach
    public void setup() throws Exception {
        Container.getInstance().scan(PackageName.repository.getPackageName());
        Container.getInstance().scan(PackageName.util.getPackageName());
        Container.getInstance().scan(PackageName.service.getPackageName());
        memberService = Container.getInstance().get(MemberServiceInterface.class);
        memberAuthService = Container.getInstance().get(MemberAuthServiceInterface.class);
    }

    @Test
    public void testSuccessfulLogin() throws Throwable {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        String sessionId = memberAuthService.login(memberLoginDTO);

        assertNotNull(sessionId);
    }

    @Test
    public void testFailedLogin() {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        assertThrows(InvalidCredentialsException.class, () -> {
            memberAuthService.login(new MemberLoginDTO("yubin111", "wrongPassword"));
        });
    }

    @Test
    public void testSuccessfulLogout() throws Throwable {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);

        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
        String sessionId = memberAuthService.login(memberLoginDTO);
        memberAuthService.logout();
        assertNull(Session.getSessionId());
    }

    @Test
    public void testSessionManagementAfterLogin() throws Throwable {
        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin13", "yubin", "yubin@gmail.com", "qwer");
        memberService.registerMember(memberDTO);
        String sessionId = memberAuthService.login(new MemberLoginDTO("yubin13", "qwer"));

        assertNotNull(sessionId);

        memberAuthService.logout();
        assertNull(memberAuthService.getRedisSessionManager().getSession(sessionId));
    }

}

