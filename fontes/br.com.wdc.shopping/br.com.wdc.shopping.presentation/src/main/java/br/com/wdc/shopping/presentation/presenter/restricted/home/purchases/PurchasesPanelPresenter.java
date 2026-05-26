package br.com.wdc.shopping.presentation.presenter.restricted.home.purchases;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractChildPresenter;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;

public class PurchasesPanelPresenter extends AbstractChildPresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(PurchasesPanelPresenter.class);

    // :: Public Class Fields

    public static Function<PurchasesPanelPresenter, CubeView> createView;

    // :: View State

    public class PurchasesPanelViewState implements ViewState {

        public List<PurchaseInfo> purchases = Collections.emptyList();
        public int page;
        public int pageSize = -1;
        public int totalCount;

    }

    public final PurchasesPanelViewState state = new PurchasesPanelViewState();

    // :: Public Instance Fields

    public final HomePresenter owner;

    // :: Internal Instance Fields

    private final PurchasesPanelService purchasesPanelService;

    // :: Constructor

    public PurchasesPanelPresenter(ShoppingApplication app, HomePresenter owner) {
        super(app);
        this.owner = owner;
        this.purchasesPanelService = new PurchasesPanelService(app);
    }

    // :: Life cycle

    @Override
    protected CubeView onCreateView() {
        return createView.apply(this);
    }

    @Override
    protected void onInitialize() {
        // Render the view so it can measure the container.
        // Data load is deferred until the view calls onItemSizeCapacityChanged().
        this.update();
    }

    // :: User Actions

    public void onPageChange(int page) {
        this.state.page = Math.max(0, page);
        this.loadPurchases();
    }

    /**
     * Called by the view when it determines how many items fit without scrolling. Only triggers a data reload if the
     * capacity actually changed.
     */
    public void onItemSizeCapacityChanged(int capacity) {
        int newPageSize = Math.max(1, capacity);
        if (newPageSize != this.state.pageSize) {
            this.state.pageSize = newPageSize;
            this.state.page = 0;
            this.loadPurchases();
        }
    }

    public void onOpenReceipt(Long purchaseId) {
        this.owner.onOpenReceipt(purchaseId);
    }

    // :: Data load

    public void loadPurchases() {
        try {
            var subject = this.app.getSubject();
            if (subject != null) {
                this.state.totalCount = purchasesPanelService.countPurchasesOfUser(subject.getId());

                int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / this.state.pageSize));
                if (this.state.page >= totalPages) {
                    this.state.page = totalPages - 1;
                }

                int offset = this.state.page * this.state.pageSize;
                this.state.purchases = purchasesPanelService.loadPurchasesOfUser(subject.getId(), offset,
                        this.state.pageSize);
                this.update();
            }
        } catch (Exception caught) {
            LOG.error("Failed to load purchases", caught);
        }
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return new CubeSkeleton() {

            @Override
            public String classId() {
                return "b3c4d5e6f7a8";
            }

            @Override
            public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
                switch (eventCode) {
                case 1 -> onOpenReceipt(CoerceUtils.asLong(formData.get("p.purchaseId")));
                case 2 -> onPageChange(CoerceUtils.asInteger(formData.get("p.page")));
                case 3 -> onItemSizeCapacityChanged(CoerceUtils.asInteger(formData.get("p.capacity")));
                default -> new AssertionError("eventCode(" + eventCode + ") not handled");
                }
            }
        };
    }

}
