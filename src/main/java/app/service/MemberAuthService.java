package app.service;

import config.annotations.Priority;
import sprout.beans.annotation.Requires;
import sprout.beans.annotation.Service;
import app.domain.Member;
import app.domain.grade.MemberGrade;
import app.dto.MemberLoginDTO;
import exception.AlreadyLoggedInException;
import exception.InvalidCredentialsException;
import exception.MemberNotFoundException;
import exception.NotLoggedInException;
import message.ExceptionMessage;
import app.service.interfaces.MemberAuthServiceInterface;
import app.service.interfaces.MemberServiceInterface;
import util.BCryptPasswordUtil;
import util.Session;
import util.interfaces.SessionManager;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@Priority(value = 1)
@Requires(dependsOn = {MemberServiceInterface.class, SessionManager.class})
public class MemberAuthService implements MemberAuthServiceInterface {

    private final MemberServiceInterface memberService;
    private final SessionManager redisSessionManager;
    private final int sessionTimeout = 3600; // 1시간

    public MemberAuthService(MemberServiceInterface memberService, SessionManager redisSessionManager) {
        this.memberService = memberService;
        this.redisSessionManager = redisSessionManager;
    }

    @Override
    public SessionManager getRedisSessionManager() {
        return this.redisSessionManager;
    }

    public String login(MemberLoginDTO memberLoginDTO) throws Throwable {
        Optional<Member> member = memberService.getMemberById(memberLoginDTO.getId());

        checkNotLogin(Session.getSessionId());

        if (member.isEmpty()) {
            throw new MemberNotFoundException(ExceptionMessage.MEMBER_NOT_FOUND);
        }

        if (!BCryptPasswordUtil.matchPassword(memberLoginDTO.getPassword(), member.get().getEncryptedPassword())) {
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
