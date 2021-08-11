package exceptions;

public class HttpEncodeException extends RuntimeException{
    public HttpEncodeException() {
        super();
    }
    public HttpEncodeException(String message) {
        super(message);
    }
}
