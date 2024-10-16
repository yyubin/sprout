package util;

import redis.clients.jedis.Jedis;

public class RedisSessionManager {

    private final Jedis jedis;
    private final Session session;

    public RedisSessionManager(Jedis jedis) {
        this.jedis = new Jedis("localhost", 6379);
        this.session = new Session(null);
    }

    public void createSession(String sessionId, String memberId, int sessionDurationInSeconds) {
        jedis.set(sessionId, memberId);
        jedis.expire(sessionId, sessionDurationInSeconds);
        this.session.setSessionId(sessionId);
    }

    public String getSession(String memberId) {
        return jedis.get(memberId);
    }

    public void deleteSession(String memberId) {
        jedis.del(memberId);
        this.session.setSessionId(null);
    }
}
