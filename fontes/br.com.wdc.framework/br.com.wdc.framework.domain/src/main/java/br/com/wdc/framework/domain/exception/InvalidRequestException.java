package br.com.wdc.framework.domain.exception;

/**
 * The request cannot be interpreted: it carries a value this side does not know how to honor.
 *
 * <p>
 * Separates "the request is wrong" from "the server broke". The REST layer returns it as 400, with the message in the
 * body — which should say which value arrived and which ones are accepted, since it is the caller's only clue.
 * </p>
 */
public class InvalidRequestException extends BusinessException {

    private static final long serialVersionUID = 8213734286231120031L;

    public InvalidRequestException(String message) {
        super(message);
    }

}
