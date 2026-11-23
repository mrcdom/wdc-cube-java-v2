package br.com.wdc.shopping.presentation.presenter.restricted.home.purchases;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractChildPresenter;
import br.com.wdc.framework.cube.ActionOf;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.Prop;
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

    public static class PurchasesPanelViewState extends ViewState {

        public final Prop<List<PurchaseInfo>> purchases = prop(Collections.emptyList());
        public final Prop<Integer> page = prop(0);
        public final Prop<Integer> pageSize = prop(-1);
        public final Prop<Integer> totalCount = prop(0);

        public ActionOf<Integer> onPageChange = ActionOf.noop();
        public ActionOf<Integer> onItemSizeCapacityChanged = ActionOf.noop();
        public ActionOf<Long> onOpenReceipt = ActionOf.noop();

    }

    public final PurchasesPanelViewState state = bind(new PurchasesPanelViewState());

    // :: Public Instance Fields

    public final HomePresenter owner;

    // :: Internal Instance Fields

    private final PurchasesPanelService purchasesPanelService;

    // :: Constructor

    public PurchasesPanelPresenter(ShoppingApplication app, HomePresenter owner) {
        super(app);
        this.owner = owner;
        this.purchasesPanelService = new PurchasesPanelService(app);
        this.state.onOpenReceipt = actionOf(1, FormParam.asLong("p.purchaseId"), owner::openReceipt);
        this.state.onPageChange = actionOf(2, FormParam.asInteger("p.page"), this::changePage);
        this.state.onItemSizeCapacityChanged = actionOf(3, FormParam.asInteger("p.capacity"), this::changeItemSizeCapacity);
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

    public void changePage(int page) {
        this.state.page.set(Math.max(0, page));
        this.loadPurchases();
    }

    /**
     * Called by the view when it determines how many items fit without scrolling. Only triggers a data reload if the
     * capacity actually changed.
     */
    private void changeItemSizeCapacity(int capacity) {
        int newPageSize = Math.max(1, capacity);
        if (newPageSize != this.state.pageSize.get()) {
            this.state.pageSize.set(newPageSize);
            this.state.page.set(0);
            this.loadPurchases();
        }
    }

    // :: Data load

    public void loadPurchases() {
        try {
            var subject = this.app.getSubject();
            if (subject != null) {
                var result = purchasesPanelService.fetchPageOfUser(
                        subject.getId(), this.state.page.get(), this.state.pageSize.get());

                // If requested page exceeds total, clamp to last page and refetch
                if (result.items().isEmpty() && result.totalPages() > 0) {
                    result = purchasesPanelService.fetchPageOfUser(
                            subject.getId(), result.totalPages() - 1, this.state.pageSize.get());
                }

                this.state.page.set(result.page());
                this.state.totalCount.set(result.totalItems());
                this.state.purchases.set(result.items());
            }
        } catch (Exception caught) {
            LOG.error("Failed to load purchases", caught);
        }
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return skeleton("b3c4d5e6f7a8");
    }

}
