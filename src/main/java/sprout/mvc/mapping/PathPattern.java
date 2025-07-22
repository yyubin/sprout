package sprout.mvc.mapping;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathPattern implements Comparable<PathPattern> {

    private final String originalPattern;
    private final Pattern regex;
    private final List<String> varNames;
    private final List<Integer> varGroups;
    private final int staticLen;
    private final int singleStarCount;
    private final int doubleStarCount;

    private static final Pattern VAR_TOKEN = Pattern.compile("\\{([^/:}]+)(?::([^}]+))?}");

    public PathPattern(String pattern) {
        this.originalPattern = Objects.requireNonNull(pattern, "Pattern must not be null");

        var re = new StringBuilder("^");
        var names = new ArrayList<String>();
        var groups = new ArrayList<Integer>();

        // Local counters for parsing
        int staticCharCount = 0;
        int singleStars = 0;
        int doubleStars = 0;
        int groupIndex = 0;

        final Matcher varMatcher = VAR_TOKEN.matcher(pattern);
        int i = 0;
        while (i < pattern.length()) {
            char ch = pattern.charAt(i);
            switch (ch) {
                case '*':
                    if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') {
                        re.append("(.+?)"); // Non-greedy match for '**'
                        doubleStars++;
                        i += 2;
                    } else {
                        re.append("([^/]+)"); // Match for '*'
                        singleStars++;
                        i++;
                    }
                    groupIndex++;
                    break;
                case '?':
                    re.append("[^/]"); // Match any character except '/'
                    i++;
                    break;
                case '{':
                    if (!varMatcher.region(i, pattern.length()).lookingAt()) {
                        throw new IllegalArgumentException("Invalid variable syntax at index " + i + " in pattern: " + pattern);
                    }
                    String varName = varMatcher.group(1);
                    String customRegex = varMatcher.group(2);
                    String expression = (customRegex != null) ? customRegex : "[^/]+";
                    re.append("(").append(expression).append(")");

                    names.add(varName);
                    groupIndex++;
                    groups.add(groupIndex);
                    i = varMatcher.end();
                    break;
                default:
                    // Any other character is treated as a static part of the path.
                    re.append(Pattern.quote(String.valueOf(ch)));
                    staticCharCount++;
                    i++;
                    break;
            }
        }
        re.append("$");

        // Assign to final fields to ensure immutability
        this.regex = Pattern.compile(re.toString());
        this.varNames = Collections.unmodifiableList(names);
        this.varGroups = Collections.unmodifiableList(groups);
        this.staticLen = staticCharCount;
        this.singleStarCount = singleStars;
        this.doubleStarCount = doubleStars;
    }

    public boolean matches(String path) {
        return this.regex.matcher(path).matches();
    }

    public Map<String, String> extractPathVariables(String path) {
        Matcher m = this.regex.matcher(path);
        if (!m.matches()) {
            return Map.of();
        }
        Map<String, String> vars = new HashMap<>();
        for (int idx = 0; idx < this.varNames.size(); idx++) {
            vars.put(this.varNames.get(idx), m.group(this.varGroups.get(idx)));
        }
        return vars;
    }

    @Override
    public int compareTo(PathPattern other) {
        int c = Integer.compare(this.doubleStarCount, other.doubleStarCount);
        if (c != 0) return c;

        c = Integer.compare(this.singleStarCount, other.singleStarCount);
        if (c != 0) return c;

        c = Integer.compare(this.varNames.size(), other.varNames.size());
        if (c != 0) return c;

        c = Integer.compare(other.staticLen, this.staticLen); // Longer static part is more specific
        if (c != 0) return c;

        // Final tie-breaker for stable sorting
        return this.originalPattern.compareTo(other.originalPattern);
    }

    public int getVariableCount() {
        return this.varNames.size();
    }

    public String getOriginalPattern() {
        return this.originalPattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathPattern that = (PathPattern) o;
        return this.originalPattern.equals(that.originalPattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.originalPattern);
    }

    @Override
    public String toString() {
        return this.originalPattern;
    }
}