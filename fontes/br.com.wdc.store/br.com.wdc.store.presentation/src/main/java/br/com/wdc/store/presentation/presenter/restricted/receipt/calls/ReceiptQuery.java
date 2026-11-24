package br.com.wdc.store.presentation.presenter.restricted.receipt.calls;

import java.util.ArrayList;
import java.util.Collections;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.resource.ResourceQuery;
import br.com.wdc.store.domain.purchase.Purchase;
import br.com.wdc.store.domain.purchase.PurchaseCriteria;
import br.com.wdc.store.domain.purchase.PurchaseRepository;
import br.com.wdc.store.presentation.exception.WrongParametersException;
import br.com.wdc.store.presentation.presenter.restricted.receipt.structs.ReceiptForm;
import br.com.wdc.store.presentation.presenter.restricted.receipt.structs.ReceiptItem;

/**
 * The receipt of one of the user's purchases: its items, in the order they were bought, and the total; {@code null}
 * when the user has no purchase with that id.
 *
 * <p>
 * The purchase id may come from the client — the event of a remote shell carries it —, so the purchase is looked up by
 * its id <b>and</b> its buyer: another user's purchase is not found.
 * </p>
 */
public class ReceiptQuery extends ResourceQuery<ReceiptForm> {

    private Long purchaseId;
    private Long userId;

    public ReceiptQuery withPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
        return this;
    }

    /** The buyer: the signed-in user. */
    public ReceiptQuery withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public ReceiptForm run() {
        if (purchaseId == null || userId == null) {
            throw new WrongParametersException();
        }

        var found = PurchaseRepository.BEAN.get().fetch(new PurchaseCriteria()
                .withPurchaseId(purchaseId)
                .withUserId(userId)
                .withProjection(projection()), 0, 1);
        var receipt = receiptOf(found.isEmpty() ? null : found.get(0));
        if (receipt != null) {
            receipt.items.sort((a, b) -> Long.compare(a.id, b.id));
        }
        return receipt;
    }

    // :: Internal

    private static Purchase projection() {
        var pv = ProjectionValues.INSTANCE;
        return new Purchase()
                .withBuyDate(pv.offsetDateTime)
                .withItems(Collections.singletonList(ReceiptItem.projection()));
    }

    private static ReceiptForm receiptOf(Purchase src) {
        if (src == null) {
            return null;
        }

        var tgt = new ReceiptForm();
        tgt.date = src.buyDate() == null ? null : src.buyDate().toInstant().toEpochMilli();
        tgt.items = new ArrayList<>();
        var total = 0.0;
        if (src.items() != null) {
            for (var purchaseItem : src.items()) {
                if (purchaseItem.price() != null) {
                    var amount = purchaseItem.amount() != null ? purchaseItem.amount() : 0;
                    total += purchaseItem.price() * amount;
                }
                tgt.items.add(ReceiptItem.create(purchaseItem));
            }
        }
        tgt.total = total;
        return tgt;
    }

}
