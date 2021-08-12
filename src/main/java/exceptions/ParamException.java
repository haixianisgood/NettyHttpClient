package exceptions;

public class ParamException extends RuntimeException{
    public ParamException() {
        super();
    }

    public ParamException(String message) {
        super(message);
    }
}
