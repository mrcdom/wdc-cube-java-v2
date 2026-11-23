package br.com.wdc.shopping.presentation;

import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.shopping.domain.ShoppingTransactions;
import br.com.wdc.shopping.domain.product.ProductRepository;
import br.com.wdc.shopping.domain.purchase.PurchaseRepository;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.shopping.domain.user.UserRepository;

/**
 * One operation of the presentation, as an object: the request is gathered with fluent {@code withXxx(...)} setters
 * and {@link #run()} answers it. A new instance per use: {@code new LoadProductCall().withProductId(id).run()}.
 * <p>
 * Each module keeps its calls in a {@code calls} package, sibling of {@code structs}. The answer is a type that already
 * exists; or a struct of the module, when other calls or the scope use it too; or an inner class of the call, when it
 * only carries this call's answer back to the caller.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class ShoppingCall<R> {

    // :: Repositories and transactions

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

    protected static TransactionService tx() {
        return ShoppingTransactions.BEAN.get();
    }

    // :: Call

    /** Answers the request gathered so far. */
    public abstract R run();

}
