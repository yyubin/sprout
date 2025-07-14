package sprout.security.web.util.matcher;

import sprout.beans.InfrastructureBean;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.mapping.PathPattern;

import java.util.Map;
import java.util.Objects;

public class AntPathRequestMatcher implements RequestMatcher, InfrastructureBean {
    private final String pattern;
    private final HttpMethod httpMethod;
    private final boolean caseSensitive;

    private final PathPattern pathPattern;

    public AntPathRequestMatcher(String pattern) {
        this(pattern, null, true);
    }

    public AntPathRequestMatcher(String pattern, HttpMethod httpMethod) {
        this(pattern, httpMethod, true);
    }

    public AntPathRequestMatcher(String pattern, HttpMethod httpMethod, boolean caseSensitive) {
        Objects.requireNonNull(pattern, "Pattern cannot be null");
        this.pattern = pattern;
        this.httpMethod = httpMethod;
        this.caseSensitive = caseSensitive;
        this.pathPattern = new PathPattern(pattern);
    }

    @Override
    public boolean matches(HttpRequest request) {
        if (httpMethod != null && !httpMethod.equals(request.getMethod())) {
            return false;
        }

        String requestPath = request.getPath();
        if (!caseSensitive) {
            requestPath = requestPath.toLowerCase();
        }

        return pathPattern.matches(requestPath);
    }

    @Override
    public MatchResult matcher(HttpRequest request) {
        if (!matches(request)) {
            return MatchResult.notMatch();
        }

        Map<String, String> variables = pathPattern.extractPathVariables(request.getPath());
        if (variables == null) {
            return MatchResult.match();
        }
        return MatchResult.match(variables);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AntPathRequestMatcher that = (AntPathRequestMatcher) o;
        return pattern.equals(that.pattern) &&
                httpMethod == that.httpMethod &&
                caseSensitive == that.caseSensitive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, httpMethod, caseSensitive);
    }

    @Override
    public String toString() {
        return "AntPathRequestMatcher{" +
                "pattern='" + pattern + '\'' +
                ", httpMethod=" + httpMethod +
                ", caseSensitive=" + caseSensitive +
                '}';
    }
}
