package br.com.wdc.shopping.presentation.presenter.restricted.cart;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.wdc.shopping.business.shared.exception.InvalidCartItemException;
import br.com.wdc.shopping.business.shared.model.Product;
import br.com.wdc.shopping.business.shared.model.Purchase;
import br.com.wdc.shopping.business.shared.model.PurchaseItem;
import br.com.wdc.shopping.business.shared.model.User;
import br.com.wdc.shopping.business.shared.repositories.PurchaseRepository;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;

public enum CartService {
    BEAN;

    public Long purchase(Long userId, List<CartItem> request) {
        var repository = PurchaseRepository.BEAN.get();

        var purchase = new Purchase();
        purchase.user = new User();
        purchase.user.id = userId;
        purchase.buyDate = OffsetDateTime.now();

        purchase.items = new ArrayList<>();
        for (var srcItem : request) {
            if (srcItem.quantity < 0) {
                throw new InvalidCartItemException();
            }

            var purchaseItem = new PurchaseItem();
            purchaseItem.product = new Product();
            purchaseItem.product.id = srcItem.id;
            purchaseItem.price = srcItem.price;
            purchaseItem.amount = srcItem.quantity;

            purchase.items.add(purchaseItem);
        }

        if (!repository.insert(purchase)) {
            throw new AssertionError("Record not inserted");
        }

        return purchase.id;
    }
}