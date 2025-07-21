package sprout.mvc.mapping;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathPattern implements Comparable<PathPattern>{
    private final String originalPattern;
    private final Pattern regexPattern;
    private final List<String> variableNames;
    private final int staticPartLength;
    private final int wildcardCount;

    public PathPattern(String originalPattern) {
        this.originalPattern = originalPattern;
        this.variableNames = new ArrayList<>();
        this.wildcardCount = countOccurrences(originalPattern, '*');
        this.regexPattern = compilePattern(originalPattern);
        this.staticPartLength = originalPattern.replaceAll("\\{[^/]+}", "").replaceAll("\\*", "").length();
    }

    private Pattern compilePattern(String pattern) {
        StringBuilder regex = new StringBuilder("^");
        String[] parts = pattern.split("(?<=\\})|(?=\\{)|(?<=\\*)|(?=\\*)");

        for (String part : parts) {
            if (part.startsWith("{") && part.endsWith("}")) {
                regex.append("([^/]+)");
                variableNames.add(part.substring(1, part.length() - 1));
            } else if (part.equals("*")) {
                regex.append("([^/]+)");
            } else {
                regex.append(Pattern.quote(part));
            }
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
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

    @Override
    public int compareTo(PathPattern other) {
        // 1. 와일드카드 개수가 적은 것이 우선
        int wildcardCompare = Integer.compare(this.wildcardCount, other.wildcardCount);
        if (wildcardCompare != 0) {
            return wildcardCompare;
        }

        // 2. 경로 변수 개수가 적은 것이 우선
        int variableCompare = Integer.compare(this.getVariableCount(), other.getVariableCount());
        if (variableCompare != 0) {
            return variableCompare;
        }

        // 3. 고정 경로(static part) 길이가 긴 것이 우선 (더 구체적)
        int staticPartCompare = Integer.compare(other.staticPartLength, this.staticPartLength);
        if (staticPartCompare != 0) {
            return staticPartCompare;
        }

        // FIX: 모든 조건이 동일할 경우, 전체 패턴 길이가 짧은 것을 우선으로 (덜 구체적)
        // 이는 Spring의 AntPathMatcher의 기본 정렬 순서와 유사하게 동작합니다.
        // 예를 들어 /a* 와 /a/* 가 있을 때 /a*를 우선시합니다.
        // 여기서는 큰 영향이 없지만, 안정성을 위해 추가합니다.
        return Integer.compare(this.originalPattern.length(), other.originalPattern.length());
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

    private int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }
}
