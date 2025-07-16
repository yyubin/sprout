package sprout.server.websocket;

import sprout.beans.annotation.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultWebSocketContainer implements WebSocketContainer{

    private final Map<String, Map<String, WebSocketSession>> sessionStore = new ConcurrentHashMap<>();

    @Override
    public void addSession(String path, WebSocketSession session) {
        sessionStore.computeIfAbsent(path, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
    }

    @Override
    public void removeSession(String path, String sessionId) {
        Map<String, WebSocketSession> sessions = sessionStore.get(path);
        if (sessions != null) {
            sessions.remove(sessionId);
        }
    }

    @Override
    public Collection<WebSocketSession> getSessions(String path) {
        return sessionStore.getOrDefault(path, Map.of()).values();
    }

    @Override
    public WebSocketSession getSession(String sessionId) {
        return sessionStore.values().stream()
                .flatMap(map -> map.values().stream())
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElse(null);
    }

}
