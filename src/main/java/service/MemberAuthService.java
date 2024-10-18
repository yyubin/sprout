package service;

import config.Container;
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

@Service
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

    public String login(MemberLoginDTO memberLoginDTO) throws MemberNotFoundException, InvalidCredentialsException {
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

    public void logout() throws NotLoggedInException {
        checkLogin(Session.getSessionId());
        redisSessionManager.deleteSession(Session.getSessionId());
    }

    public MemberGrade checkAuthority(String sessionId) throws NotLoggedInException {
        String memberId = checkLoginAndgetMemberId(sessionId);
        return memberService.getMemberById(memberId)
                .map(Member::getGrade)
                .orElseThrow(() -> new MemberNotFoundException(ExceptionMessage.MEMBER_NOT_FOUND));
    }

    private void checkLogin(String sessionId) throws NotLoggedInException {
        if (redisSessionManager.getSession(sessionId) == null) {
            throw new NotLoggedInException(ExceptionMessage.NOT_LOGGED_IN);
        }
    }

    private void checkNotLogin(String sessionId) throws AlreadyLoggedInException {
        if (sessionId == null) {
            return;
        }
        if (redisSessionManager.getSession(sessionId) != null) {
            throw new AlreadyLoggedInException(ExceptionMessage.ALREADY_LOGGED_IN);
        }
    }

    private String checkLoginAndgetMemberId(String sessionId) throws NotLoggedInException {
        String memberId = redisSessionManager.getSession(sessionId);
        if (redisSessionManager.getSession(sessionId) == null) {
            throw new NotLoggedInException(ExceptionMessage.NOT_LOGGED_IN);
        }
        return memberId;
    }


}
