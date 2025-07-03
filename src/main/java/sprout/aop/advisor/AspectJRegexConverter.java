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

        // 1. 점(.) 문자를 정규식 이스케이프 처리
        String regex = aspectJExpr.replace(".", ESCAPED_DOT);

        // 2. ".." 패턴 처리: 0개 이상의 패키지 또는 타입 세그먼트에 매치
        // 예: com..service -> com\..*\\.service
        regex = TWO_DOTS_PATTERN.matcher(regex).replaceAll(".*");


        // 3. "*" 패턴 처리: 단일 패키지 세그먼트, 클래스 이름, 메서드 이름 등에 매치

        // 단독 * -> .*
        regex = STANDALONE_ASTERISK_PATTERN.matcher(regex).replaceAll(".*");

        // * 로 시작하는 경우: ^[^.]+
        regex = START_ASTERISK_PATTERN.matcher(regex).replaceAll("[^.]+?");

        // * 로 끝나는 경우: [^.]+$
        regex = END_ASTERISK_PATTERN.matcher(regex).replaceAll("[^.]+?");

        // 중간에 * 가 있는 경우: [^.]*
        regex = SINGLE_ASTERISK_PATTERN.matcher(regex).replaceAll("$1[^.]+?$2");

        return regex;
    }

}
