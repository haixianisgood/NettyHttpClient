package exceptions;

public class NettyProxyException extends RuntimeException{
    public NettyProxyException() {
        super();
    }

    public NettyProxyException(String message) {
        super(message);
    }
}
