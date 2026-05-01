package br.com.wdc.shopping.presentation.presenter.restricted.receipt;

import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.exception.WrongParametersException;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptForm;

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

        var receipt = ReceiptForm.create(repo.fetchById(purchaseId, ReceiptForm.projection()));
        if (receipt != null) {
            receipt.items.sort((a, b) -> Long.compare(a.id, b.id));
        }
        return receipt;
    }
}