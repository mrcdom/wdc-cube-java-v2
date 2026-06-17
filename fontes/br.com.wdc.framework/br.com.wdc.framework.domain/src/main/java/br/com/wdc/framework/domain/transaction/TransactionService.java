package br.com.wdc.framework.domain.transaction;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Controle programático de transação no estilo CMT (Container-Managed Transaction) do EJB.
 *
 * <p>
 * A fronteira da transação é sempre o {@code work} fornecido: o framework abre o escopo conforme o atributo de
 * propagação, executa o trabalho e, ao final:
 * </p>
 * <ul>
 * <li><b>COMMITA</b> em retorno normal — exceto se {@link TransactionContext#setRollbackOnly()} tiver sido chamado;</li>
 * <li><b>ROLLBACK</b> se o trabalho lançar qualquer exceção (que é repropagada).</li>
 * </ul>
 *
 * <p>
 * Cada atributo de propagação tem duas formas, com <b>nomes distintos</b> para evitar ambiguidade de overload entre
 * lambdas void e lambdas com valor:
 * </p>
 * <ul>
 * <li>{@code xxx(Consumer<TransactionContext>)} — trabalho <b>sem retorno</b>;</li>
 * <li>{@code xxxCall(Function<TransactionContext, T>)} — trabalho <b>com retorno</b>.</li>
 * </ul>
 * O {@link TransactionContext} da transação corrente é entregue ao trabalho para marcação de rollback e introspecção.
 *
 * <p>
 * Exemplos:
 * </p>
 * <pre>{@code
 * TransactionService.BEAN.get().required(tx -> {
 *     sistemaRepository.insert(sistema);
 *     if (regraDeNegocioFalhou) {
 *         tx.setRollbackOnly();          // aborta sem lançar exceção
 *     }
 * });
 *
 * var lista = TransactionService.BEAN.get().requiredCall(tx -> sistemaRepository.fetch(criteria));
 * }</pre>
 *
 * <p>
 * <b>Afinidade de thread:</b> a transação é presa à thread corrente (ThreadLocal) e é single-thread. Não compartilhe a
 * conexão entre threads; subtarefas concorrentes devem abrir a sua própria via {@link #requiresNew}.
 * </p>
 */
public interface TransactionService {

    AtomicReference<TransactionService> BEAN = new AtomicReference<>();

    /** REQUIRED — junta-se à transação ativa ou abre uma nova quando não há. */
    void required(Consumer<TransactionContext> work);

    /** REQUIRED — junta-se à transação ativa ou abre uma nova quando não há. */
    <T> T requiredCall(Function<TransactionContext, T> work);

    /** REQUIRES_NEW — suspende a transação ativa (segunda conexão no modo JDBC), abre uma nova e retoma ao final. */
    void requiresNew(Consumer<TransactionContext> work);

    /** REQUIRES_NEW — suspende a transação ativa (segunda conexão no modo JDBC), abre uma nova e retoma ao final. */
    <T> T requiresNewCall(Function<TransactionContext, T> work);

    /** MANDATORY — exige transação ativa; lança {@link TransactionRequiredException} se não houver. */
    void mandatory(Consumer<TransactionContext> work);

    /** MANDATORY — exige transação ativa; lança {@link TransactionRequiredException} se não houver. */
    <T> T mandatoryCall(Function<TransactionContext, T> work);

    /** SUPPORTS — participa da transação ativa, se houver; caso contrário, executa sem transação. */
    void supports(Consumer<TransactionContext> work);

    /** SUPPORTS — participa da transação ativa, se houver; caso contrário, executa sem transação. */
    <T> T supportsCall(Function<TransactionContext, T> work);

    /** NOT_SUPPORTED — suspende a transação ativa e executa sem transação; retoma ao final. */
    void notSupported(Consumer<TransactionContext> work);

    /** NOT_SUPPORTED — suspende a transação ativa e executa sem transação; retoma ao final. */
    <T> T notSupportedCall(Function<TransactionContext, T> work);

    /** NEVER — proíbe transação ativa; lança {@link TransactionNotAllowedException} se houver uma. */
    void never(Consumer<TransactionContext> work);

    /** NEVER — proíbe transação ativa; lança {@link TransactionNotAllowedException} se houver uma. */
    <T> T neverCall(Function<TransactionContext, T> work);

}
