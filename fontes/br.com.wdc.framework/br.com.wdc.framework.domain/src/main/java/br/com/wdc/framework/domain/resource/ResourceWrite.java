package br.com.wdc.framework.domain.resource;

import java.util.Objects;
import java.util.function.Supplier;

import br.com.wdc.framework.domain.transaction.TransactionService;

/**
 * A write: {@link #run()} opens the transaction (or joins the caller's) and does the work in {@link #write()}, so no
 * write goes out of one.
 * <p>
 * Only the write verbs extend it ({@link ResourceCreate}, {@link ResourceSave}, {@link ResourceUpdate},
 * {@link ResourceDelete}): the constructor is package-private. A call extends the verb, implements the method named
 * after it, and hands over the {@link TransactionService} of its module, which is read when the call runs.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class ResourceWrite<R> extends ResourceCall<R> {

    private final Supplier<TransactionService> transactions;

    ResourceWrite(Supplier<TransactionService> transactions) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public final R run() {
        return transactions.get().requiredCall(tx -> write());
    }

    /** The write itself, inside the transaction; each verb hands it to the method named after it. */
    abstract R write();

}
