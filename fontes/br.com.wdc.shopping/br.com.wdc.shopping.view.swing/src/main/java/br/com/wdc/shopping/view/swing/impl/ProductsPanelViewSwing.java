package br.com.wdc.shopping.view.swing.impl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.impl.home.ProductItemViewSwing;
import br.com.wdc.shopping.view.swing.util.Styles;
import br.com.wdc.shopping.view.swing.util.WrapPanel;

public class ProductsPanelViewSwing extends AbstractViewSwing<ProductsPanelPresenter> {

    private final ProductsPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<ProductItemViewSwing> itemViewList = new ArrayList<>();
    private BiConsumer<List<ProductInfo>, List<ProductItemViewSwing>> contentSlot;

    public ProductsPanelViewSwing(ShoppingSwingApplication app, ProductsPanelPresenter presenter) {
        super("products-panel", app, presenter, new JPanel(new BorderLayout()));
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.itemIdx = 0;
        this.itemViewList.clear();
        this.contentSlot = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }
        this.contentSlot.accept(this.state.products, this.itemViewList);
    }

    private void initialRender() {
        this.element.setOpaque(false);
        this.element.setBorder(new EmptyBorder(8, 8, 8, 8));

        var caption = new JLabel("PRODUTOS");
        caption.setFont(Styles.FONT_TITLE);
        caption.setForeground(Styles.FG_TEXT);
        caption.setBorder(new EmptyBorder(8, 4, 12, 4));
        this.element.add(caption, BorderLayout.NORTH);

        var flowPane = new WrapPanel();
        flowPane.setHgap(12);
        flowPane.setVgap(12);
        this.contentSlot = this.newListSlot(flowPane, this::newItemView, this::updateItem);
        this.element.add(flowPane, BorderLayout.CENTER);
    }

    private ProductItemViewSwing newItemView() {
        return new ProductItemViewSwing(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ProductItemViewSwing itemView, ProductInfo state) {
        itemView.setState(state, false);
        itemView.doUpdate();
    }
}
