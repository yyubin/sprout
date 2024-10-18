package util;

import config.annotations.Component;
import redis.clients.jedis.Jedis;

@Component
public class RedisSessionManager {

    private final Jedis jedis;

    public RedisSessionManager() {
        this.jedis = new Jedis("localhost", 6379);
        Session.setSessionId(null);
    }

    public void createSession(String sessionId, String memberId, int sessionDurationInSeconds) {
        jedis.set(sessionId, memberId);
        jedis.expire(sessionId, sessionDurationInSeconds);
        Session.setSessionId(sessionId);
    }

    public String getSession(String sessionId) {
        return jedis.get(sessionId);
    }

    public void deleteSession(String sessionId) {
        jedis.del(sessionId);
        Session.setSessionId(null);
    }
}
