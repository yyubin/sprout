package util;

import sprout.beans.annotation.Component;
import org.yaml.snakeyaml.Yaml;
import redis.clients.jedis.Jedis;
import util.interfaces.SessionManager;

import java.io.InputStream;
import java.util.Map;

@Component
public class RedisSessionManager implements SessionManager {

    private Jedis jedis;

    public RedisSessionManager() {
        loadConfiguration();
    }

    private void loadConfiguration() {
        Yaml yaml = new Yaml();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            Map<String, Object> data = yaml.load(input);
            String host = (String) ((Map<String, Object>) data.get("redis")).get("host");
            int port = (int) ((Map<String, Object>) data.get("redis")).get("port");

            this.jedis = new Jedis(host, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
