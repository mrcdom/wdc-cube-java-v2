package br.com.wdc.framework.domain.transaction;

/**
 * Handle da transação corrente, entregue ao trabalho executado por {@link TransactionService}.
 *
 * <p>
 * Não expõe ciclo de vida: abrir, commitar e desfazer são responsabilidade do framework (a fronteira é o lambda). O
 * trabalho usa este handle apenas para marcar rollback e consultar o estado da transação.
 * </p>
 */
public interface TransactionContext {

    /**
     * Marca a transação corrente para rollback. Operação <b>one-way</b>: uma vez marcada, não há como desmarcar, e o
     * framework fará rollback ao final do escopo.
     *
     * @throws IllegalStateException se não houver transação ativa (ex.: dentro de {@code never()} ou
     *                               {@code notSupported()}).
     */
    void setRollbackOnly();

    /** @return {@code true} se a transação corrente já está marcada para rollback. */
    boolean isRollbackOnly();

    /** @return {@code true} se há transação ativa neste escopo (false em {@code never}/{@code notSupported}, ou em
     *          {@code supports} quando não havia transação ao entrar). */
    boolean isActive();

}
