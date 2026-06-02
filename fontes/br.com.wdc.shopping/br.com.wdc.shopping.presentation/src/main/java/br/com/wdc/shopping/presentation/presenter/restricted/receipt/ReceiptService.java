package br.com.wdc.shopping.presentation.presenter.restricted.receipt;

import java.util.ArrayList;
import java.util.Collections;

import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.exception.WrongParametersException;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptForm;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;

public class ReceiptService {

    private final PurchaseRepository repo;

    public ReceiptService(ShoppingApplication app) {
        this.repo = app.getPurchaseRepository();
    }

    public ReceiptService(PurchaseRepository repo) {
        this.repo = repo;
    }

    public ReceiptForm loadReceipt(Long purchaseId) {
        if (purchaseId == null) {
            throw new WrongParametersException();
        }

        var receipt = mapPurchaseToReceiptForm(repo.fetchById(purchaseId, projection()));
        if (receipt != null) {
            receipt.items.sort((a, b) -> Long.compare(a.id, b.id));
        }
        return receipt;
    }

    private Purchase projection() {
        var pv = ProjectionValues.INSTANCE;

        var prj = new Purchase();
        prj.buyDate = pv.offsetDateTime;
        prj.items = Collections.singletonList(ReceiptItem.projection());

        return prj;
    }

    public ReceiptForm mapPurchaseToReceiptForm(Purchase src) {
        if (src == null) {
            return null;
        }

        var tgt = new ReceiptForm();

        tgt.date = src.buyDate == null ? null : src.buyDate.toInstant().toEpochMilli();
        tgt.items = new ArrayList<>();

        var total = 0.0;
        if (src.items != null) {
            for (var purchaseItem : src.items) {
                if (purchaseItem.price != null) {
                    var amount = purchaseItem.amount != null ? purchaseItem.amount : 0;
                    total += purchaseItem.price * amount;
                }

                tgt.items.add(ReceiptItem.create(purchaseItem));
            }
        }
        tgt.total = total;

        return tgt;
    }
}