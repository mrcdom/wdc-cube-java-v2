package br.com.wdc.shopping.presentation.presenter.restricted.receipt;

import java.util.Objects;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.exception.PurchaseNotFoundException;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.calls.ReceiptQuery;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptForm;

public class ReceiptPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(ReceiptPresenter.class);

    // :: Public Instance Fields

    public final ReceiptScope state = bind(new ReceiptScope());

    // :: Internal Instance Fields

    private Long purchaseId;
    private ScopeSlot ownerSlot;

    // :: Constructor

    public ReceiptPresenter(ShoppingApplication app) {
        super(app);
        this.state.onOpenProducts = action(1, () -> new HomeKeys(this.app).flip());
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        var keys = new ReceiptKeys(this.app, intent);
        var pPurchaseId = keys.purchaseId() != null ? keys.purchaseId() : this.purchaseId;
        if (pPurchaseId == null) {
            // No usable id in the address: nothing to show; go to the home, with the intent in transit
            new HomeKeys(this.app, intent).flip();
            return false;
        }
        this.state.notifySuccess.set(keys.purchaseMade());

        if (this.state.receipt.get() == null || !Objects.equals(pPurchaseId, this.purchaseId)) {
            var receipt = this.loadReceipt(pPurchaseId);
            this.purchaseId = pPurchaseId;
            this.state.receipt.set(receipt);
        }

        // Read on every call: a reused presenter goes where its parent puts it now, not where it was first put
        this.ownerSlot = keys.parentSlot();
        if (initialization) {

            if (this.state.receipt.get() == null) {
                throw new AssertionError("Missing receipt");
            }
            update();
        }

        this.ownerSlot.setScope(this.state);

        return true;
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        new ReceiptKeys(this.app, intent)
                .withPurchaseId(this.purchaseId);
    }

    // :: Data Loaders

    private ReceiptForm loadReceipt(Long purchaseId) {
        var receipt = new ReceiptQuery()
                .withPurchaseId(purchaseId)
                .run();
        if (receipt == null) {
            throw new PurchaseNotFoundException();
        }
        return receipt;
    }

}
