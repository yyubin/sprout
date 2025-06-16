package sprout.mvc.argument.builtins;

import app.util.Session;
import sprout.beans.annotation.Component;
import sprout.mvc.argument.ArgumentResolver;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

@Component
public class SessionResolver implements ArgumentResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return parameter.getType().equals(Session.class) ||
                (parameter.getType().equals(String.class) && parameter.getName().equals("sessionId"));
    }

    @Override
    public Object resolve(Parameter parameter, HttpRequest<Map<String, Object>> request) throws Exception {
        if (parameter.getType().equals(Session.class)) {
            return null;
        } else if (parameter.getType().equals(String.class) && parameter.getName().equals("sessionId")) {
            return request.getSessionId();
        }
        return null;
    }
}
