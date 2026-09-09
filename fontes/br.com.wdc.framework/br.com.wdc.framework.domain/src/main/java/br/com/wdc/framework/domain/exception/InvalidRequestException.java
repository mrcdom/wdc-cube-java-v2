package br.com.wdc.framework.domain.exception;

/**
 * O pedido não é interpretável: veio um valor que este lado não sabe honrar.
 *
 * <p>
 * Existe para separar "o pedido está errado" de "o servidor quebrou". Sem ela, um cliente defasado — que envia um nome
 * de ordenação que já não existe, por exemplo — recebe 500 e a mensagem genérica que o acompanha, e quem investiga não
 * tem por onde começar: a falha aparece na tela como uma lista vazia. Com ela, a resposta é 400 e diz qual valor
 * chegou e quais são aceitos.
 * </p>
 */
public class InvalidRequestException extends BusinessException {

    private static final long serialVersionUID = 8213734286231120031L;

    public InvalidRequestException(String message) {
        super(message);
    }

}
