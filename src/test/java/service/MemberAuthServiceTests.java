//package service;
//
//import domain.Member;
//import dto.MemberLoginDTO;
//import dto.MemberRegisterDTO;
//import exception.InvalidCredentialsException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import redis.clients.jedis.Jedis;
//import util.RedisSessionManager;
//
//import java.time.LocalDate;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class MemberAuthServiceTests {
//
//    private MemberAuthService memberAuthService;
//    private MemberService memberService;
//
//    @BeforeEach
//    public void setup() {
//        memberService = new MemberService();
//        Jedis jedis = new Jedis("localhost", 6379);
//        RedisSessionManager redisSessionManager = new RedisSessionManager(jedis);
//        memberAuthService = new MemberAuthService(memberService, redisSessionManager);
//    }
//
//    @Test
//    public void testSuccessfulLogin() {
//        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
//        memberService.registerMember(memberDTO);
//
//        MemberLoginDTO memberLoginDTO = new MemberLoginDTO("yubin111", "qwer");
//        String sessionId = memberAuthService.login(memberLoginDTO);
//
//        assertNotNull(sessionId);
//    }
//
//    @Test
//    public void testFailedLogin() {
//        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin111", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
//        memberService.registerMember(memberDTO);
//
//        assertThrows(InvalidCredentialsException.class, () -> {
//            memberAuthService.login(new MemberLoginDTO("yubin111", "wrongPassword"));
//        });
//    }
//
//    @Test
//    public void testSessionManagementAfterLogin() {
//        MemberRegisterDTO memberDTO = new MemberRegisterDTO("yubin13", "yubin", "yubin@gmail.com", "qwer", LocalDate.now());
//        memberService.registerMember(memberDTO);
//        String sessionId = memberAuthService.login(new MemberLoginDTO("yubin13", "qwer"));
//
//        assertNotNull(sessionId);
//
//        memberAuthService.logout(sessionId);
//        assertNull(memberAuthService.getRedisSessionManager().getSession(sessionId));
//    }
//
//}

// 요청객체 까지 만들고 다시 테스트해야함
