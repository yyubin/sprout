package app.exception;

public class UnsupportedHttpMethod extends RuntimeException {

    public UnsupportedHttpMethod() {
        super();
    }

    public UnsupportedHttpMethod(String message) {
        super(message);
    }

    public UnsupportedHttpMethod(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedHttpMethod(Throwable cause) {
        super(cause);
    }

}
