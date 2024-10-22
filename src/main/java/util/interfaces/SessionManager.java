package util.interfaces;

public interface SessionManager {

    void createSession(String sessionId, String memberId, int sessionDurationInSeconds);

    String getSession(String sessionId);

    void deleteSession(String sessionId);

}
