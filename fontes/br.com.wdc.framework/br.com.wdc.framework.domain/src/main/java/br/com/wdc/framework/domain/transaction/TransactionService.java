package br.com.wdc.framework.domain.transaction;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Programmatic transaction control in the style of EJB CMT (Container-Managed Transaction).
 *
 * <p>
 * The transaction boundary is always the supplied {@code work}: the framework opens the scope according to the
 * propagation attribute, runs the work and, at the end:
 * </p>
 * <ul>
 * <li><b>COMMITS</b> on normal return — unless {@link TransactionContext#setRollbackOnly()} was called;</li>
 * <li><b>ROLLS BACK</b> if the work throws any exception (which is rethrown).</li>
 * </ul>
 *
 * <p>
 * Each propagation attribute comes in two forms, with <b>distinct names</b> to avoid overload ambiguity between void
 * lambdas and value-returning lambdas:
 * </p>
 * <ul>
 * <li>{@code xxx(Consumer<TransactionContext>)} — work <b>without a result</b>;</li>
 * <li>{@code xxxCall(Function<TransactionContext, T>)} — work <b>with a result</b>.</li>
 * </ul>
 * The {@link TransactionContext} of the current transaction is handed to the work for rollback marking and
 * introspection.
 *
 * <p>
 * Examples:
 * </p>
 * <pre>{@code
 * shoppingTransactions.required(tx -> {     // per-module holder (e.g. ShoppingTransactions.BEAN.get())
 *     systemRepository.insert(system);
 *     if (businessRuleFailed) {
 *         tx.setRollbackOnly();          // aborts without throwing
 *     }
 * });
 *
 * var list = shoppingTransactions.requiredCall(tx -> systemRepository.fetch(criteria));
 * }</pre>
 *
 * <p>
 * <b>No global holder:</b> there is no {@code TransactionService.BEAN}. Each module exposes its own holder (e.g.
 * {@code ShoppingTransactions} in {@code shopping.domain}), populated by the backend with an instance bound to that
 * module's {@code DataSource}. This way modules with distinct databases have independent transactions.
 * </p>
 *
 * <p>
 * <b>Thread affinity:</b> the transaction is bound to the current thread (ThreadLocal) and is single-threaded. Do not
 * share the connection across threads; concurrent subtasks must open their own via {@link #requiresNew}.
 * </p>
 */
public interface TransactionService {

    /** REQUIRED — joins the active transaction or opens a new one when there is none. */
    void required(Consumer<TransactionContext> work);

    /** REQUIRED — joins the active transaction or opens a new one when there is none. */
    <T> T requiredCall(Function<TransactionContext, T> work);

    /** REQUIRES_NEW — suspends the active transaction (second connection in JDBC mode), opens a new one and resumes at the end. */
    void requiresNew(Consumer<TransactionContext> work);

    /** REQUIRES_NEW — suspends the active transaction (second connection in JDBC mode), opens a new one and resumes at the end. */
    <T> T requiresNewCall(Function<TransactionContext, T> work);

    /** MANDATORY — requires an active transaction; throws {@link TransactionRequiredException} if there is none. */
    void mandatory(Consumer<TransactionContext> work);

    /** MANDATORY — requires an active transaction; throws {@link TransactionRequiredException} if there is none. */
    <T> T mandatoryCall(Function<TransactionContext, T> work);

    /** SUPPORTS — takes part in the active transaction, if any; otherwise runs without a transaction. */
    void supports(Consumer<TransactionContext> work);

    /** SUPPORTS — takes part in the active transaction, if any; otherwise runs without a transaction. */
    <T> T supportsCall(Function<TransactionContext, T> work);

    /** NOT_SUPPORTED — suspends the active transaction and runs without one; resumes at the end. */
    void notSupported(Consumer<TransactionContext> work);

    /** NOT_SUPPORTED — suspends the active transaction and runs without one; resumes at the end. */
    <T> T notSupportedCall(Function<TransactionContext, T> work);

    /** NEVER — forbids an active transaction; throws {@link TransactionNotAllowedException} if there is one. */
    void never(Consumer<TransactionContext> work);

    /** NEVER — forbids an active transaction; throws {@link TransactionNotAllowedException} if there is one. */
    <T> T neverCall(Function<TransactionContext, T> work);

}
