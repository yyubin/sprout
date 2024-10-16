package util;

import redis.clients.jedis.Jedis;

public class RedisSessionManager {

    private final Jedis jedis;

    public RedisSessionManager(Jedis jedis) {
        this.jedis = new Jedis("localhost", 6379);
    }

    public void createSession(String sessionId, String memberId, int sessionDurationInSeconds) {
        jedis.set(sessionId, memberId);
        jedis.expire(sessionId, sessionDurationInSeconds);
    }

    public String getSession(String sessionId) {
        return jedis.get(sessionId);
    }

    public void deleteSession(String sessionId) {
        jedis.del(sessionId);
    }
}
