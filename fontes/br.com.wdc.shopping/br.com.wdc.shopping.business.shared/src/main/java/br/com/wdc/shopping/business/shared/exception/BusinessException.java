package br.com.wdc.shopping.business.shared.exception;

public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static BusinessException wrap(String message, Exception e) {
        if (e instanceof BusinessException exn) {
            return exn;
        }
        var exn = new BusinessException(message);
        exn.addSuppressed(e);
        return exn;
    }

    public BusinessException() {
        super();
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }

}
