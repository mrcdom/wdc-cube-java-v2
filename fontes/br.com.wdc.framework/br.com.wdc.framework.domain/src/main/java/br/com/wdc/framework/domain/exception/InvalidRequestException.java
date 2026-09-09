package br.com.wdc.framework.domain.exception;

/**
 * O pedido não é interpretável: veio um valor que este lado não sabe honrar.
 *
 * <p>
 * Separa "o pedido está errado" de "o servidor quebrou". A camada REST a devolve como 400, com a mensagem no corpo —
 * que deve dizer qual valor chegou e quais são aceitos, já que é a única pista de quem chamou.
 * </p>
 */
public class InvalidRequestException extends BusinessException {

    private static final long serialVersionUID = 8213734286231120031L;

    public InvalidRequestException(String message) {
        super(message);
    }

}
