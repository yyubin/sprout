package message;

public class ExceptionMessage {
    private ExceptionMessage() {
    }

    public static final String ALREADY_LOGGED_IN = "이미 로그인한 상태입니다.";
    public static final String NOT_LOGGED_IN = "로그인 되어 있지 않습니다.";
    public static final String MEMBER_ID_ALREADY_EXISTS = "이미 존재하는 아이디 입니다.";
    public static final String MEMBER_EMAIL_ALREADY_EXISTS = "이미 존재하는 이메일 입니다.";
    public static final String MEMBER_NOT_FOUND = "존재하지 않는 회원입니다.";
    public static final String INVALID_CREDENTIALS = "비밀번호가 일치하지 않습니다.";

    public static final String NOT_FOUND_POST_WITH_POST_ID = "글 번호에 해당하는 글이 없습니다! 글번호 :";
    public static final String NOT_FOUND_BOARD_WITH_BOARD_ID = "게시판 번호에 해당하는 게신판을 찾을 수 없습니다! 게시판 번호 : ";

    public static final String UNAUTHORIZED_CREATE_BOARD = "게시판 생성 권한이 없습니다.";
    public static final String UNAUTHORIZED_CREATE_POST = "게시글 생성 권한이 없습니다.";
    public static final String UNAUTHORIZED_POST = "해당 글에 대한 권한이 없습니다.";

    public static final String ALREADY_USED_BOARD_NAME = "이미 사용중인 게시판 이름입니다.";

    public static final String BAD_REQUEST = "잘못된 요청입니다.";
    public static final String UNSUPPORTED_HTTP_METHOD = "지원하지 않는 http 메서드입니다.";
    public static final String NO_MATCHING_PATH = "지원하지 않는 path 입니다.";
}
