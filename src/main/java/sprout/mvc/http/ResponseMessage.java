package sprout.mvc.http;

public class ResponseMessage {

    private ResponseMessage() {
    }

    public static final String SUCCESS = "OK";
    public static final String CREATED = "Created!";
    public static final String ACCEPT_NO_CONTENT = "Accept No Content";

    public static final String NOT_FOUND = "Not Found";
    public static final String BAD_REQUEST = "Bad Request";
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String FORBIDDEN = "Forbidden";
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
}
