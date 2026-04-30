package br.com.wdc.shopping.presentation.presenter.restricted.home.purchases;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.AbstractChildPresenter;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeService;

public class PurchasesPanelPresenter extends AbstractChildPresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Logger LOG = LoggerFactory.getLogger(PurchasesPanelPresenter.class);

    // :: Public Class Fields

    public static final int DEFAULT_PAGE_SIZE = 3;
    public static Function<PurchasesPanelPresenter, CubeView> createView;

    // :: Public Instance Fields

    public final HomePresenter owner;
    public final PurchasesPanelViewState state = new PurchasesPanelViewState();

    // :: Contructor

    public PurchasesPanelPresenter(ShoppingApplication app, HomePresenter owner) {
        super(app);
        this.owner = owner;
    }

    // :: Life cycle

    @Override
    protected CubeView onCreateView() {
        return createView.apply(this);
    }

    @Override
    protected void onInitialize() {
        this.loadPurchases();
    }

    // :: User Actions

    public void onPageChange(int page) {
        this.state.page = Math.max(0, page);
        this.loadPurchases();
    }

    public void onPageSizeChange(int pageSize) {
        this.state.pageSize = Math.max(1, pageSize);
        this.state.page = 0;
        this.loadPurchases();
    }

    public void onOpenReceipt(Long purchaseId) {
        this.owner.onOpenReceipt(purchaseId);
    }

    // :: Data load

    public void loadPurchases() {
        try {
            var service = HomeService.BEAN;

            var subject = this.app.getSubject();
            if (subject != null) {
                this.state.totalCount = service.countPurchasesOfUser(subject.getId());

                int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / this.state.pageSize));
                if (this.state.page >= totalPages) {
                    this.state.page = totalPages - 1;
                }

                int offset = this.state.page * this.state.pageSize;
                this.state.purchases = service.loadPurchasesOfUser(subject.getId(), offset, this.state.pageSize);
                this.update();
            }
        } catch (Exception caught) {
            LOG.error("Failed to load purchases", caught);
        }
    }

}
