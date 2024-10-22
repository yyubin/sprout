package service.interfaces;

import domain.grade.MemberGrade;
import dto.MemberLoginDTO;
import util.RedisSessionManager;
import util.SessionManager;

public interface MemberAuthServiceInterface {
    SessionManager getRedisSessionManager();

    String login(MemberLoginDTO memberLoginDTO) throws Throwable;
    void logout() throws Throwable;
    MemberGrade checkAuthority(String sessionId) throws Throwable;
}
