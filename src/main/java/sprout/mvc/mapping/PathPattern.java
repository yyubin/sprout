package sprout.mvc.mapping;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathPattern {
    private final String originalPattern;
    private final Pattern regexPattern;
    private final List<String> variableNames;

    public PathPattern(String originalPattern) {
        this.originalPattern = originalPattern;
        this.variableNames = new ArrayList<>();
        this.regexPattern = compilePattern(originalPattern);
    }

    private Pattern compilePattern(String pattern) {
        StringBuilder regex = new StringBuilder();
        Matcher matcher = Pattern.compile("\\{([^/{}]+)}").matcher(pattern);
        int lastEnd = 0;

        while (matcher.find()) {
            regex.append(Pattern.quote(pattern.substring(lastEnd, matcher.start())));
            regex.append("([^/]+)");
            variableNames.add(matcher.group(1));
            lastEnd = matcher.end();
        }

        regex.append(Pattern.quote(pattern.substring(lastEnd)));
        return Pattern.compile("^" + regex + "$");
    }

    public boolean matches(String path) {
        return regexPattern.matcher(path).matches();
    }

    public Map<String, String> extractPathVariables(String path) {
        Matcher matcher = regexPattern.matcher(path);
        Map<String, String> result = new HashMap<>();

        if (matcher.matches()) {
            for (int i = 0; i < variableNames.size(); i++) {
                result.put(variableNames.get(i), matcher.group(i + 1));
            }
        }

        return result;
    }

    public int getVariableCount() {
        return variableNames.size();
    }

    public String getOriginalPattern() {
        return originalPattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathPattern other)) return false;
        return Objects.equals(this.originalPattern, other.originalPattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalPattern);
    }

    @Override
    public String toString() {
        return originalPattern;
    }
}
