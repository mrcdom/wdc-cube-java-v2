package br.com.wdc.shopping.view.jfx.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.jfx.AbstractViewJfx;
import br.com.wdc.shopping.view.jfx.ShoppingJfxApplication;
import br.com.wdc.shopping.view.jfx.impl.home.ProductItemViewJfx;
import br.com.wdc.shopping.view.jfx.util.JfxDom;
import javafx.scene.layout.VBox;

public class ProductsPanelViewJfx extends AbstractViewJfx<ProductsPanelPresenter> {

    private final ProductsPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<ProductItemViewJfx> itemViewList = new ArrayList<>();
    private BiConsumer<List<ProductInfo>, List<ProductItemViewJfx>> contentSlot;

    public ProductsPanelViewJfx(ShoppingJfxApplication app, ProductsPanelPresenter presenter) {
        super("products-panel", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            JfxDom.render((VBox) this.element, this::initialRender);
            this.notRendered = false;
        }
        this.contentSlot.accept(this.state.products, this.itemViewList);
    }

    private void initialRender(JfxDom dom, VBox pane0) {
        pane0.getStyleClass().add("products-panel");

        dom.label(label -> {
            label.getStyleClass().add("caption");
            label.setText("PRODUTOS");
        });

        dom.flowPane(pane1 -> {
            pane1.setHgap(12);
            pane1.setVgap(12);
            this.contentSlot = this.newListSlot(pane1, this::newItemView, this::updateItem);
        });
    }

    private ProductItemViewJfx newItemView() {
        return new ProductItemViewJfx(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ProductItemViewJfx itemView, ProductInfo state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }
}
