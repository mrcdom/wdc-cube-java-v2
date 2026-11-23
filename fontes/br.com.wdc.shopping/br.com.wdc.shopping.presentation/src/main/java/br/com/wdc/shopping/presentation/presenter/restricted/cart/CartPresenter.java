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
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.domain.exception.InvalidCartItemException;
import br.com.wdc.framework.domain.exception.OfflineException;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;

public class CartPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(CartPresenter.class);

    // :: Public class Fields

    public static Function<CartPresenter, CubeView> createView;

    // :: Public Instance Fields

    public static class CartViewState extends ViewState {

        public final Prop<List<CartItem>> items = prop();
        public final Prop<Integer> errorCode = prop(0);
        public final Prop<String> errorMessage = prop();

        public ActionOf2<Long, Integer> onModifyQuantity = ActionOf2.noop();
        public ActionOf<Long> onRemoveProduct = ActionOf.noop();
        public Action onBuy = Action.NOOP;
        public Action onOpenProducts = Action.NOOP;

    }

    public final CartViewState state = bind(new CartViewState());

    // :: Internal Instance Fields

    private CartManager cart;
    private CubeViewSlot ownerSlot;
    private Registration pendingErrorClear;

    // :: Constructor

    public CartPresenter(ShoppingApplication app) {
        super(app);
        this.cart = app.getCart();
        this.state.items.set(Collections.emptyList());
        this.state.onModifyQuantity = actionOf2(this::modifyQuantity);
        this.state.onRemoveProduct = actionOf(this::removeProduct);
        this.state.onBuy = action(this::buy);
        this.state.onOpenProducts = action(() -> new HomeKeys(this.app).flip());
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        this.state.items.set(this.cart.getCartItems());

        // Read on every call: a reused presenter goes where its parent puts it now, not where it was first put
        this.ownerSlot = new CartKeys(this.app, intent).parentSlot();
        if (initialization) {
            this.view = createView.apply(this);
        }

        this.ownerSlot.setView(this.view);

        return true;
    }

    // :: User Actions

    private void modifyQuantity(Long productId, Integer quantity) {
        if (productId == null) {
            this.errorCodigoDeProdutoMalFormatado();
            LOG.warn("modifyQuantity.errorCodigoDeProdutoMalFormatado: {}", this.state.errorMessage.get());
            return;
        }

        if (quantity == null) {
            this.errorValorQuantidadeMalFormatado();
            LOG.warn("modifyQuantity.errorValorQuantidadeMalFormatado: {}", this.state.errorMessage.get());
            return;
        }

        if (quantity < 1) {
            this.alertThereIsItemWhichValueIsLessThanOne();
            LOG.warn("modifyQuantity.alertThereIsItemWhichValueIsLessThanOne: {}", this.state.errorMessage.get());
            return;
        }

        var found = this.cart.modifyProductQuantity(productId, quantity);
        if (!found) {
            this.alertProductNotFound();
            LOG.warn("modifyQuantity.alertProductNotFound: {}", this.state.errorMessage.get());
        } else {
            this.state.errorCode.set(0);
            this.state.errorMessage.set(null);
            this.state.items.set(this.cart.getCartItems());
            // The cart changed the item in place: to the prop, the list is the same
            this.update();
        }
    }

    private void removeProduct(Long productId) {
        if (productId == null) {
            this.errorCodigoDeProdutoMalFormatado();
            LOG.warn("removeProduct: {}", this.state.errorMessage.get());
            return;
        }

        var modified = this.cart.removeProduct(productId);
        if (modified) {
            if (this.cart.getSize() == 0) {
                new HomeKeys(this.app).flip();
            } else {
                this.state.items.set(this.cart.getCartItems());
                // A view over the cart's own list: to the prop, it is the same list
                this.update();
            }
        }
    }

    private void buy() {
        if (this.cart.getSize() == 0) {
            this.alertPurchaseOfEmptyCart();
            LOG.warn("buy: {}", this.state.errorMessage.get());
            return;
        }

        try {
            var purchaseId = this.cart.commit(this.app.getSubject());

            new ReceiptKeys(this.app).withPurchaseId(purchaseId).withPurchaseMade(true).flip();
        } catch (InvalidCartItemException caught) {
            this.alertThereIsItemWhichValueIsLessThanOne();
            LOG.error("buy.alertThereIsItemWhichValueIsLessThanOne: {}", this.state.errorMessage.get(), caught);
        } catch (OfflineException caught) {
            this.alertDatabaseOffline();
            LOG.error("buy.alertDatabaseOffline: {}", this.state.errorMessage.get(), caught);
        }
    }

    // :: Message Methods

    private void clearErrorAfterDelay() {
        if (this.pendingErrorClear != null) {
            this.pendingErrorClear.remove();
        }
        this.pendingErrorClear = ScheduledExecutor.BEAN.get().schedule(() -> {
            this.state.errorCode.set(0);
            this.state.errorMessage.set(null);
        }, Duration.ofSeconds(3));
    }

    protected void alertThereIsItemWhichValueIsLessThanOne() {
        this.state.errorCode.set(1);
        this.state.errorMessage.set("Deve existir pelo menos um item no carrinhro para se efetivar uma compra");
        this.clearErrorAfterDelay();
    }

    protected void alertProductNotFound() {
        this.state.errorCode.set(2);
        this.state.errorMessage.set("Produdo não localizado na base dados.");
        this.clearErrorAfterDelay();
    }

    protected void alertPurchaseOfEmptyCart() {
        this.state.errorCode.set(3);
        this.state.errorMessage.set("Existem produtos com menos de um item na quantidade. Impossível comprar.");
        this.clearErrorAfterDelay();
    }

    protected void alertDatabaseOffline() {
        this.state.errorCode.set(4);
        this.state.errorMessage.set("O banco de dados encontra-se fora do ar no momento. Aguarde alguns instantes e tente novamente.");
        this.clearErrorAfterDelay();
    }

    protected void errorCodigoDeProdutoMalFormatado() {
        this.state.errorCode.set(5);
        this.state.errorMessage.set("Código do produto mal formado.");
        this.clearErrorAfterDelay();
    }

    protected void errorValorQuantidadeMalFormatado() {
        this.state.errorCode.set(6);
        this.state.errorMessage.set("Valor da quantiade está mal formado.");
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
