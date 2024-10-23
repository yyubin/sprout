package message;

public class PrintResultMessage {
    private PrintResultMessage() {
    }

    public static final String ACCOUNTS_SIGNUP_SUCCESS = "회원 가입이 완료되었습니다.";
    public static final String ACCOUNTS_LOGIN_SUCCESS = "로그인이 성공했습니다. sessionId : ";
    public static final String ACCOUNTS_LOGOUT_SUCCESS = "로그아웃에 성공했습니다.";
    public static final String ACCOUNTS_MEMBER_EDIT = "회원 수정이 완료되었습니다.";
    public static final String ACCOUNTS_DELETE_SUCCESS = "회원 탈퇴가 완료되었습니다.";

    public static final String BOARD_CREATE_SUCCESS = "게시판 생성이 성공하였습니다.";
    public static final String BOARD_UPDATE_SUCCESS = "게시판 수정이 성공하였습니다.";
    public static final String BOARD_DELETE_SUCCESS = "게시판 삭제가 성공하였습니다.";

    public static final String POST_CREATE_SUCCESS = "게시글 생성이 성공하였습니다.";
    public static final String POST_UPDATE_SUCCESS = "게시글 수정이 성공하였습니다.";
    public static final String POST_DELETE_SUCCESS = "게시글 삭제가 성공하였습니다.";

    public static final String COMMENT_CREATE_SUCCESS = "댓글 생성이 성공하였습니다.";
    public static final String COMMENT_UPDATE_SUCCESS = "댓글 수정이 성공하였습니다.";
    public static final String COMMENT_DELETE_SUCCESS = "댓글 삭제가 성공하였습니다.";

}
