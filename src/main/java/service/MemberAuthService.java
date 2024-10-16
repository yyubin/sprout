package service;

import domain.Member;
import dto.MemberLoginDTO;
import exception.InvalidCredentialsException;
import exception.MemberNotFoundException;
import exception.NotLoggedInException;
import message.ExceptionMessage;
import redis.clients.jedis.Jedis;
import repository.InMemoryMemberRepository;
import util.PasswordUtil;
import util.RedisSessionManager;

import java.util.Optional;
import java.util.UUID;

public class MemberAuthService {

    private final MemberService memberService;
    private final RedisSessionManager redisSessionManager;
    private final int sessionTimeout = 3600; // 1시간

    public MemberAuthService(MemberService memberService , RedisSessionManager redisSessionManager) {
        this.memberService = memberService;
        this.redisSessionManager = redisSessionManager;
    }

    public RedisSessionManager getRedisSessionManager() {
        return redisSessionManager;
    }

    public String login(MemberLoginDTO memberLoginDTO) throws MemberNotFoundException, InvalidCredentialsException {
        Optional<Member> member = memberService.getMemberById(memberLoginDTO.getId());

        if (member.isEmpty()) {
            throw new MemberNotFoundException(ExceptionMessage.MEMBER_NOT_FOUND);
        }

        if (!PasswordUtil.matchPassword(memberLoginDTO.getPassword(), member.get().getEncryptedPassword())) {
            throw new InvalidCredentialsException(ExceptionMessage.INVALID_CREDENTIALS);
        }

        String sessionId = UUID.randomUUID().toString();
        redisSessionManager.createSession(sessionId, member.get().getId(), sessionTimeout);

        return sessionId;
    }

    public void logout(String sessionId) throws NotLoggedInException {
        if (redisSessionManager.getSession(sessionId) == null) {
            throw new NotLoggedInException(ExceptionMessage.NOT_LOGGED_IN);
        }
        redisSessionManager.deleteSession(sessionId);
    }

}
