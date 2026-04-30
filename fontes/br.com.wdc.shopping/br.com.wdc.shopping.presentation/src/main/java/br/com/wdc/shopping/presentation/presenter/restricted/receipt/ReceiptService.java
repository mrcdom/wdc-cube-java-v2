package br.com.wdc.shopping.presentation.presenter.restricted.receipt;

import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.presentation.exception.WrongParametersException;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptForm;

public enum ReceiptService {
    BEAN;

    public ReceiptForm loadReceipt(Long purchaseId) {
        if (purchaseId == null) {
            throw new WrongParametersException();
        }

        var receipt = ReceiptForm.create(PurchaseRepository.BEAN.get()
                .fetchById(purchaseId, ReceiptForm.projection()));
        if (receipt != null) {
            receipt.items.sort((a, b) -> Long.compare(a.id, b.id));
        }
        return receipt;
    }
}