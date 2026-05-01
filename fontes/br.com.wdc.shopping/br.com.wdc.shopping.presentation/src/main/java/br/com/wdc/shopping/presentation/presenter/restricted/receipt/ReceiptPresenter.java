package br.com.wdc.shopping.presentation.presenter.restricted.receipt;

import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.shopping.presentation.PlaceAttributes;
import br.com.wdc.shopping.presentation.PlaceParameters;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.exception.PurchaseNotFoundException;
import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptForm;

public class ReceiptPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Logger LOG = LoggerFactory.getLogger(ReceiptPresenter.class.getName());

    // :: Public Class Fields

    public static Function<ReceiptPresenter, CubeView> createView;

    // :: Public Instance Fields

    public final ReceiptViewState state = new ReceiptViewState();

    // :: Internal Instance Fields

    private final ReceiptService receiptService;
    private Long purchaseId;
    private CubeViewSlot ownerSlot;

    // :: Constructor

    public ReceiptPresenter(ShoppingApplication app) {
        super(app);
        this.receiptService = new ReceiptService(app);
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        this.state.notifySuccess = Boolean.TRUE.equals(intent.getAttribute(PlaceAttributes.ATTR_PURCHASE_MADE));

        var pPurchaseId = intent.getParameterAsLong(PlaceParameters.PURCHASE_ID, this.purchaseId);
        if (pPurchaseId == null) {
            throw new AssertionError("Missing PURCHASE_ID");
        }

        if (this.state.receipt == null || !Objects.equals(pPurchaseId, this.purchaseId)) {
            var receipt = this.loadReceipt(pPurchaseId);
            this.purchaseId = pPurchaseId;
            this.state.receipt = receipt;
            update();
        }

        if (initialization) {
            this.ownerSlot = intent.getViewSlot(PlaceAttributes.SLOT_OWNER);

            if (this.state.receipt == null) {
                throw new AssertionError("Missing receipt");
            }

            this.view = createView.apply(this);
            update();
        }

        this.ownerSlot.setView(this.view);

        return true;
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        if (this.purchaseId != null) {
            intent.setParameter(PlaceParameters.PURCHASE_ID, this.purchaseId);
        }
    }

    // :: User Actions

    public void onPrint() {
        // Not implemented
    }

    public void onOpenProducts() {
        try {
            Routes.home(this.app);
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, "Going to restricted home place", caught);
        }
    }

    // :: Data Loaders

    private ReceiptForm loadReceipt(Long purchaseId) {
        var receipt = receiptService.loadReceipt(purchaseId);
        if (receipt == null) {
            throw new PurchaseNotFoundException();
        }
        return receipt;
    }

}
