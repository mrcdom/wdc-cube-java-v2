package br.com.wdc.shopping.presentation.presenter.restricted.cart;

import java.util.Collections;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.shopping.business.shared.exception.InvalidCartItemException;
import br.com.wdc.shopping.business.shared.exception.OfflineException;
import br.com.wdc.shopping.presentation.PlaceAttributes;
import br.com.wdc.shopping.presentation.PlaceParameters;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.Routes;

public class CartPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Logger LOG = LoggerFactory.getLogger(CartPresenter.class.getName());

    // :: Public class Fields

    public static Function<CartPresenter, CubeView> createView;

    // :: Public Instance Fields

    public final CartViewState state = new CartViewState();

    // :: Internal Instance Fields

    private CartManager cart;
    private CubeViewSlot ownerSlot;

    // :: Constructor

    public CartPresenter(ShoppingApplication app) {
        super(app);
        this.cart = app.getCart();
        this.state.items = Collections.emptyList();
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

    public void onModifyQuantity(Long productId, Integer quantity) {
        try {
            if (productId == null) {
                this.errorCodigoDeProdutoMalFormatado();
                LOG.warn("onModifyQuantity.errorCodigoDeProdutoMalFormatado: {}", this.state.errorMessage);
                return;
            }

            if (quantity == null) {
                this.errorValorQuantidadeMalFormatado();
                LOG.warn("onModifyQuantity.errorValorQuantidadeMalFormatado: {}", this.state.errorMessage);
                return;
            }

            if (quantity < 1) {
                this.alertThereIsItemWhichValueIsLessThanOne();
                LOG.warn("onModifyQuantity.alertThereIsItemWhichValueIsLessThanOne: {}", this.state.errorMessage);
                return;
            }

            var found = this.cart.modifyProductQuantity(productId, quantity);
            if (!found) {
                this.alertProductNotFound();
                LOG.warn("onModifyQuantity.alertProductNotFound: {}", this.state.errorMessage);
            }
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, "Removing a prouduct", caught);
        }
    }

    public void onRemoveProduct(Long productId) {
        try {
            if (productId == null) {
                this.errorCodigoDeProdutoMalFormatado();
                LOG.warn("onRemoveProduct: {}", this.state.errorMessage);
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
        } catch (Exception caught) {
            app.alertUnexpectedError(LOG, "Removing a prouduct", caught);
        }
    }

    public void onBuy() {
        try {
            if (this.cart.getSize() == 0) {
                this.alertPurchaseOfEmptyCart();
                LOG.warn("onBuy: {}", this.state.errorMessage);
                return;
            }

            var purchaseId = this.cart.commit(this.app.getSubject());

            final var intent = this.app.newIntent();
            intent.setParameter(PlaceParameters.PURCHASE_ID, purchaseId);
            intent.setAttribute(PlaceAttributes.ATTR_PURCHASE_MADE, Boolean.TRUE);
            Routes.receipt(this.app, intent);
        } catch (Exception caught) {
            if (caught instanceof InvalidCartItemException) {
                this.alertThereIsItemWhichValueIsLessThanOne();
                LOG.error("onBuy.alertThereIsItemWhichValueIsLessThanOne: {}", this.state.errorMessage, caught);
                return;
            }

            if (caught instanceof OfflineException) {
                this.alertDatabaseOffline();
                LOG.error("onBuy.alertDatabaseOffline: {}", this.state.errorMessage, caught);
                return;
            }

            this.app.alertUnexpectedError(LOG, "Buying an product", caught);
        }
    }

    public void onOpenProducts() {
        try {
            Routes.home(this.app);
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, "Going to root restricted place", caught);
        }
    }

    // :: Message Methods

    protected void alertThereIsItemWhichValueIsLessThanOne() {
        this.state.errorCode = 1;
        this.state.errorMessage = "Deve existir pelo menos um item no carrinhro para se efetivar uma compra";
        this.update();
    }

    protected void alertProductNotFound() {
        this.state.errorCode = 2;
        this.state.errorMessage = "Produdo não localizado na base dados.";
        this.update();
    }

    protected void alertPurchaseOfEmptyCart() {
        this.state.errorCode = 3;
        this.state.errorMessage = "Existem produtos com menos de um item na quantidade. Impossível comprar.";
        this.update();
    }

    protected void alertDatabaseOffline() {
        this.state.errorCode = 4;
        this.state.errorMessage = "O banco de dados encontra-se fora do ar no momento. Aguarde alguns instantes e tente novamente.";
        this.update();
    }

    protected void errorCodigoDeProdutoMalFormatado() {
        this.state.errorCode = 5;
        this.state.errorMessage = "Código do produto mal formado.";
        this.update();
    }

    protected void errorValorQuantidadeMalFormatado() {
        this.state.errorCode = 6;
        this.state.errorMessage = "Valor da quantiade está mal formado.";
        this.update();
    }

}
