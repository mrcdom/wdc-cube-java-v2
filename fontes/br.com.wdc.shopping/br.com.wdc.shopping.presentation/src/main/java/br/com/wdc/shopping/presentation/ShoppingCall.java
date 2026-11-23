package br.com.wdc.shopping.presentation;

import br.com.wdc.shopping.domain.product.ProductRepository;
import br.com.wdc.shopping.domain.purchase.PurchaseRepository;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.shopping.domain.user.UserRepository;

/**
 * One operation of the presentation, as an object: the request is gathered with fluent {@code withXxx(...)} setters
 * and {@link #run()} answers it. A new instance per use: {@code new ProductQuery().withProductId(id).run()}.
 * <p>
 * What it does names it: a read is an {@code XxxQuery} ({@link ShoppingQuery}), a write an {@code XxxSave}
 * ({@link ShoppingSave}), and what is neither stays an {@code XxxCall} (the login, which authenticates). Each module
 * keeps them in a {@code calls} package, sibling of {@code structs}. The answer is a type that already exists; or a
 * struct of the module, when others use it too; or an inner class of the call, when it only carries this call's answer
 * back to the caller.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class ShoppingCall<R> {

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
