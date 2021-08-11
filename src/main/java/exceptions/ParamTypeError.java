package exceptions;

public class ParamTypeError extends RuntimeException{
    public ParamTypeError() {
        super();
    }

    public ParamTypeError(String message) {
        super(message);
    }
}
