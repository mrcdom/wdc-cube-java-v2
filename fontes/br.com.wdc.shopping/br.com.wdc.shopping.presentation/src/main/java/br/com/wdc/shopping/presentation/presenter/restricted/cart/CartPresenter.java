package br.com.wdc.shopping.presentation.presenter.restricted.cart;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.ActionOf;
import br.com.wdc.framework.cube.ActionOf2;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.domain.exception.InvalidCartItemException;
import br.com.wdc.framework.domain.exception.OfflineException;
import br.com.wdc.shopping.presentation.PlaceAttributes;
import br.com.wdc.shopping.presentation.PlaceParameters;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;

public class CartPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(CartPresenter.class);

    // :: Public class Fields

    public static Function<CartPresenter, CubeView> createView;

    // :: Public Instance Fields

    public static class CartViewState implements ViewState {

        public List<CartItem> items;
        public int errorCode;
        public String errorMessage;

        public ActionOf2<Long, Integer> onModifyQuantity = ActionOf2.noop();
        public ActionOf<Long> onRemoveProduct = ActionOf.noop();
        public Action onBuy = Action.NOOP;
        public Action onOpenProducts = Action.NOOP;

    }

    public final CartViewState state = new CartViewState();

    // :: Internal Instance Fields

    private CartManager cart;
    private CubeViewSlot ownerSlot;
    private Registration pendingErrorClear;

    // :: Constructor

    public CartPresenter(ShoppingApplication app) {
        super(app);
        this.cart = app.getCart();
        this.state.items = Collections.emptyList();
        this.state.onModifyQuantity = actionOf2(this::modifyQuantity);
        this.state.onRemoveProduct = actionOf(this::removeProduct);
        this.state.onBuy = action(this::buy);
        this.state.onOpenProducts = action(() -> Routes.home(this.app));
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        this.state.items = this.cart.getCartItems();

        if (initialization) {
            this.ownerSlot = intent.getViewSlot(PlaceAttributes.SLOT_OWNER);
            this.view = createView.apply(this);
        }

        this.ownerSlot.setView(this.view);

        return true;
    }

    // :: User Actions

    private void modifyQuantity(Long productId, Integer quantity) {
        if (productId == null) {
            this.errorCodigoDeProdutoMalFormatado();
            LOG.warn("modifyQuantity.errorCodigoDeProdutoMalFormatado: {}", this.state.errorMessage);
            return;
        }

        if (quantity == null) {
            this.errorValorQuantidadeMalFormatado();
            LOG.warn("modifyQuantity.errorValorQuantidadeMalFormatado: {}", this.state.errorMessage);
            return;
        }

        if (quantity < 1) {
            this.alertThereIsItemWhichValueIsLessThanOne();
            LOG.warn("modifyQuantity.alertThereIsItemWhichValueIsLessThanOne: {}", this.state.errorMessage);
            return;
        }

        var found = this.cart.modifyProductQuantity(productId, quantity);
        if (!found) {
            this.alertProductNotFound();
            LOG.warn("modifyQuantity.alertProductNotFound: {}", this.state.errorMessage);
        } else {
            this.state.errorCode = 0;
            this.state.errorMessage = null;
            this.state.items = this.cart.getCartItems();
            this.update();
        }
    }

    private void removeProduct(Long productId) {
        if (productId == null) {
            this.errorCodigoDeProdutoMalFormatado();
            LOG.warn("removeProduct: {}", this.state.errorMessage);
            return;
        }

        var modified = this.cart.removeProduct(productId);
        if (modified) {
            if (this.cart.getSize() == 0) {
                Routes.home(this.app);
            } else {
                this.state.items = this.cart.getCartItems();
                this.update();
            }
        }
    }

    private void buy() {
        if (this.cart.getSize() == 0) {
            this.alertPurchaseOfEmptyCart();
            LOG.warn("buy: {}", this.state.errorMessage);
            return;
        }

        try {
            var purchaseId = this.cart.commit(this.app.getSubject());

            final var intent = this.app.newIntent();
            intent.setParameter(PlaceParameters.PURCHASE_ID, purchaseId);
            intent.setAttribute(PlaceAttributes.ATTR_PURCHASE_MADE, Boolean.TRUE);
            Routes.receipt(this.app, intent);
        } catch (InvalidCartItemException caught) {
            this.alertThereIsItemWhichValueIsLessThanOne();
            LOG.error("buy.alertThereIsItemWhichValueIsLessThanOne: {}", this.state.errorMessage, caught);
        } catch (OfflineException caught) {
            this.alertDatabaseOffline();
            LOG.error("buy.alertDatabaseOffline: {}", this.state.errorMessage, caught);
        }
    }

    // :: Message Methods

    private void clearErrorAfterDelay() {
        if (this.pendingErrorClear != null) {
            this.pendingErrorClear.remove();
        }
        this.pendingErrorClear = ScheduledExecutor.BEAN.get().schedule(() -> {
            this.state.errorCode = 0;
            this.state.errorMessage = null;
            this.update();
        }, Duration.ofSeconds(3));
    }

    protected void alertThereIsItemWhichValueIsLessThanOne() {
        this.state.errorCode = 1;
        this.state.errorMessage = "Deve existir pelo menos um item no carrinhro para se efetivar uma compra";
        this.update();
        this.clearErrorAfterDelay();
    }

    protected void alertProductNotFound() {
        this.state.errorCode = 2;
        this.state.errorMessage = "Produdo não localizado na base dados.";
        this.update();
        this.clearErrorAfterDelay();
    }

    protected void alertPurchaseOfEmptyCart() {
        this.state.errorCode = 3;
        this.state.errorMessage = "Existem produtos com menos de um item na quantidade. Impossível comprar.";
        this.update();
        this.clearErrorAfterDelay();
    }

    protected void alertDatabaseOffline() {
        this.state.errorCode = 4;
        this.state.errorMessage = "O banco de dados encontra-se fora do ar no momento. Aguarde alguns instantes e tente novamente.";
        this.update();
        this.clearErrorAfterDelay();
    }

    protected void errorCodigoDeProdutoMalFormatado() {
        this.state.errorCode = 5;
        this.state.errorMessage = "Código do produto mal formado.";
        this.update();
        this.clearErrorAfterDelay();
    }

    protected void errorValorQuantidadeMalFormatado() {
        this.state.errorCode = 6;
        this.state.errorMessage = "Valor da quantiade está mal formado.";
        this.update();
        this.clearErrorAfterDelay();
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return new CubeSkeleton() {

            @Override
            public String classId() {
                return "7eb485e5f843";
            }

            @Override
            public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
                switch (eventCode) {
                case 1 -> state.onBuy.run();
                case 2 -> state.onRemoveProduct.run(CoerceUtils.asLong(formData.get("p.productId")));
                case 3 -> state.onOpenProducts.run();
                case 4 -> state.onModifyQuantity.run(CoerceUtils.asLong(formData.get("p.productId")), CoerceUtils.asInteger(formData.get("p.quantity")));
                default -> new AssertionError("eventCode(" + eventCode + ") not handled");
                }
            }
        };
    }

}
