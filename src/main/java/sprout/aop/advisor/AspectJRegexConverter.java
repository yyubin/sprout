package sprout.aop.advisor;

import java.util.regex.Pattern;

public class AspectJRegexConverter {
    private static final String ESCAPED_DOT = "\\.";
    private static final Pattern SINGLE_ASTERISK_PATTERN = Pattern.compile("([^\\.])\\*([^\\.])"); // . 뒤나 앞이 아닌 *
    private static final Pattern START_ASTERISK_PATTERN = Pattern.compile("^\\*([^\\.])"); // *로 시작
    private static final Pattern END_ASTERISK_PATTERN = Pattern.compile("([^\\.])\\*$"); // *로 끝남
    private static final Pattern STANDALONE_ASTERISK_PATTERN = Pattern.compile("^\\*$"); // 단독 *
    private static final Pattern TWO_DOTS_PATTERN = Pattern.compile("\\.\\.\\.?"); // .. 또는 ...

    private AspectJRegexConverter() {}

    public static String toRegex(String aspectJExpr) {
        if (aspectJExpr == null || aspectJExpr.isEmpty()) {
            return "";
        }
        String regex = TWO_DOTS_PATTERN.matcher(aspectJExpr).replaceAll(".*");
        regex = regex.replace(".", ESCAPED_DOT);
        regex = STANDALONE_ASTERISK_PATTERN.matcher(regex).replaceAll(".*");
        regex = START_ASTERISK_PATTERN     .matcher(regex).replaceAll("[^.]+?");
        regex = END_ASTERISK_PATTERN       .matcher(regex).replaceAll("[^.]+?");
        regex = SINGLE_ASTERISK_PATTERN    .matcher(regex).replaceAll("$1[^.]+?$2");

        return regex;
    }

}
