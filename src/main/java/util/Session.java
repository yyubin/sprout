package util;

public class Session {

    private static String sessionId = null;

    private Session() {}

    public static String getSessionId() {
        return sessionId;
    }

    public static void setSessionId(String sessionId) {
        Session.sessionId = sessionId;
    }
}
