package message;

public class PrintResultMessage {
    private PrintResultMessage() {
    }

    public static final String ACCOUNTS_SIGNUP_SUCCESS = "회원 가입이 완료되었습니다.";
    public static final String ACCOUNTS_LOGIN_SUCCESS = "로그인이 성공했습니다. sessionId : ";
    public static final String ACCOUNTS_LOGOUT_SUCCESS = "로그아웃에 성공했습니다.";
    public static final String ACCOUNTS_MEMBER_EDIT = "회원 수정이 완료되었습니다.";
    public static final String ACCOUNTS_DELETE_SUCCESS = "회원 탈퇴가 완료되었습니다.";
}
