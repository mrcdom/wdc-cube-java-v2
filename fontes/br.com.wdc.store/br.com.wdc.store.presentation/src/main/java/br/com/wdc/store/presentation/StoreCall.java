package br.com.wdc.store.presentation;

import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.domain.purchase.PurchaseRepository;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.store.domain.user.UserRepository;

/**
 * One operation of the presentation, as an object: the request is gathered with fluent {@code withXxx(...)} setters
 * and {@link #run()} answers it. A new instance per use: {@code new ProductQuery().withProductId(id).run()}.
 * <p>
 * What it does names it, with a verb as the suffix, in the spirit of the HTTP methods: {@code XxxQuery}
 * ({@link StoreQuery}, the GET), {@code XxxCreate} ({@link StoreCreate}, POST), {@code XxxSave} ({@link StoreSave},
 * PUT), {@code XxxUpdate} ({@link StoreUpdate}, PATCH) and {@code XxxDelete} ({@link StoreDelete}, DELETE); the write
 * verbs share {@link StoreWrite}, which opens the transaction. What fits none of them gets a verb of its own that
 * says what it does and extends this class directly ({@code CredentialsCheck}, which authenticates the login). Each
 * module keeps them in a {@code calls} package, sibling of {@code structs}. The answer is a type that already exists;
 * or a struct of the module, when others use it too; or an inner class of the call, when it only carries this call's
 * answer back to the caller.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class StoreCall<R> {

    // :: Repositories

    protected static UserRepository userRepository() {
        return UserRepository.BEAN.get();
    }

    protected static ProductRepository productRepository() {
        return ProductRepository.BEAN.get();
    }

    protected static PurchaseRepository purchaseRepository() {
        return PurchaseRepository.BEAN.get();
    }

    protected static PurchaseItemRepository purchaseItemRepository() {
        return PurchaseItemRepository.BEAN.get();
    }

    // :: Call

    /** Answers the request gathered so far. */
    public abstract R run();

}
