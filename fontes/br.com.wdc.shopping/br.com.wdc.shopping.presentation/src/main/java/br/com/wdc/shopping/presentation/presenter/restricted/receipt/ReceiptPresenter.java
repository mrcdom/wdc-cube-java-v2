package br.com.wdc.shopping.presentation.presenter.restricted.receipt;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.shopping.presentation.exception.PurchaseNotFoundException;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptForm;

public class ReceiptPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(ReceiptPresenter.class);

    // :: Public Class Fields

    public static Function<ReceiptPresenter, CubeView> createView;

    // :: Public Instance Fields

    public static class ReceiptViewState implements ViewState {

        public boolean notifySuccess;
        public ReceiptForm receipt;

        public Action onOpenProducts = Action.NOOP;

    }

    public final ReceiptViewState state = new ReceiptViewState();

    // :: Internal Instance Fields

    private final ReceiptService receiptService;
    private Long purchaseId;
    private CubeViewSlot ownerSlot;

    // :: Constructor

    public ReceiptPresenter(ShoppingApplication app) {
        super(app);
        this.receiptService = new ReceiptService(app);
        this.state.onOpenProducts = action(() -> new HomeKeys(this.app).flip());
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
        this.state.notifySuccess = keys.purchaseMade();

        if (this.state.receipt == null || !Objects.equals(pPurchaseId, this.purchaseId)) {
            var receipt = this.loadReceipt(pPurchaseId);
            this.purchaseId = pPurchaseId;
            this.state.receipt = receipt;
            update();
        }

        // Read on every call: a reused presenter goes where its parent puts it now, not where it was first put
        this.ownerSlot = keys.parentSlot();
        if (initialization) {

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
        new ReceiptKeys(this.app, intent).withPurchaseId(this.purchaseId);
    }

    // :: Data Loaders

    private ReceiptForm loadReceipt(Long purchaseId) {
        var receipt = receiptService.loadReceipt(purchaseId);
        if (receipt == null) {
            throw new PurchaseNotFoundException();
        }
        return receipt;
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return new CubeSkeleton() {

            @Override
            public String classId() {
                return "e8d0bd8ae3bc";
            }

            @Override
            public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
                if (eventCode == 1) {
                    state.onOpenProducts.run();
                }
            }
        };
    }

}
