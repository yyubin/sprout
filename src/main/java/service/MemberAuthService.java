package service;

import config.Container;
import config.annotations.Priority;
import config.annotations.Requires;
import config.annotations.Service;
import domain.Member;
import domain.grade.MemberGrade;
import dto.MemberLoginDTO;
import exception.AlreadyLoggedInException;
import exception.InvalidCredentialsException;
import exception.MemberNotFoundException;
import exception.NotLoggedInException;
import message.ExceptionMessage;
import redis.clients.jedis.Jedis;
import repository.InMemoryMemberRepository;
import util.PasswordUtil;
import util.RedisSessionManager;
import util.Session;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@Priority(value = 1)
@Requires(dependsOn = {MemberService.class, RedisSessionManager.class})
public class MemberAuthService {

    private final MemberService memberService;
    private final RedisSessionManager redisSessionManager;
    private final int sessionTimeout = 3600; // 1시간

    public MemberAuthService(MemberService memberService, RedisSessionManager redisSessionManager) {
        this.memberService = memberService;
        this.redisSessionManager = redisSessionManager;
    }

    public RedisSessionManager getRedisSessionManager() {
        return redisSessionManager;
    }

    public String login(MemberLoginDTO memberLoginDTO) throws Throwable {
        Optional<Member> member = memberService.getMemberById(memberLoginDTO.getId());

        checkNotLogin(Session.getSessionId());

        if (member.isEmpty()) {
            throw new MemberNotFoundException(ExceptionMessage.MEMBER_NOT_FOUND);
        }

        if (!PasswordUtil.matchPassword(memberLoginDTO.getPassword(), member.get().getEncryptedPassword())) {
            throw new InvalidCredentialsException(ExceptionMessage.INVALID_CREDENTIALS);
        }

        String newSessionId = UUID.randomUUID().toString();
        redisSessionManager.createSession(newSessionId, member.get().getId(), sessionTimeout);

        return newSessionId;
    }

    public void logout() throws Throwable {
        checkLogin(Session.getSessionId());
        redisSessionManager.deleteSession(Session.getSessionId());
    }

    public MemberGrade checkAuthority(String sessionId) throws Throwable {
        String memberId = checkLoginAndGetMemberId(sessionId);
        return memberService.getMemberById(memberId)
                .map(Member::getGrade)
                .orElseThrow(() -> new MemberNotFoundException(ExceptionMessage.MEMBER_NOT_FOUND));
    }

    private void checkLogin(String sessionId) throws Throwable {
        checkExists(sessionId,
                () -> redisSessionManager.getSession(sessionId) == null,
                () -> new NotLoggedInException(ExceptionMessage.NOT_LOGGED_IN));
    }

    private void checkNotLogin(String sessionId) throws Throwable {
        if (sessionId == null) {
            return;
        }
        checkExists(sessionId,
                () -> redisSessionManager.getSession(sessionId) != null,
                () -> new AlreadyLoggedInException(ExceptionMessage.ALREADY_LOGGED_IN));
    }

    private String checkLoginAndGetMemberId(String sessionId) throws Throwable {
        String memberId = redisSessionManager.getSession(sessionId);
        checkExists(memberId,
                () -> memberId == null,
                () -> new NotLoggedInException(ExceptionMessage.NOT_LOGGED_IN));
        return memberId;
    }

    private void checkMemberExists(Optional<Member> member) throws Throwable {
        checkExists(member,
                member::isEmpty,
                () -> new MemberNotFoundException(ExceptionMessage.MEMBER_NOT_FOUND));
    }

    private void checkExists(Object value,
                             Supplier<Boolean> checkFunction,
                             Supplier<Throwable> exceptionSupplier) throws Throwable {
        if (checkFunction.get()) {
            throw exceptionSupplier.get();
        }
    }

}
