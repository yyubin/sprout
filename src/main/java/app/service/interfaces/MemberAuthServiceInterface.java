package app.service.interfaces;

import app.domain.grade.MemberGrade;
import app.dto.MemberLoginDTO;
import app.util.interfaces.SessionManager;

public interface MemberAuthServiceInterface {
    SessionManager getRedisSessionManager();

    String login(MemberLoginDTO memberLoginDTO) throws Throwable;
    void logout() throws Throwable;
    MemberGrade checkAuthority(String sessionId) throws Throwable;
}
