package br.com.wdc.shopping.test.mock.viewimpl;

import java.util.Date;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

@SuppressWarnings({ "java:S106", "java:S1192" })
public class ReceiptViewMock extends AbstractViewMock<ReceiptPresenter> {

    public static ReceiptViewMock cast(CubeView view) {
        var cls = ReceiptViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (ReceiptViewMock) view;
    }

    public ReceiptViewState state;

    public ReceiptViewMock(ShoppingApplicationMock app, ReceiptPresenter presenter) {
        super(app, presenter);
        this.state = presenter.state;
    }

    public void printRecibo() {
        if (this.state.notifySuccess) {
            System.out.println("Compra efetuada com sucesso");
            System.out.println();
        }

        System.out.println("Imprima seu recibo:");

        System.out.println("------------------------------------------------------------");
        System.out.println("WeDoCode Shopping - SUA COMPRA CERTA NA INTERNET");
        System.out.println("Recibo de compra");
        System.out.println("Data: " + new Date(this.state.receipt.date));
        System.out.println("------------------------------------------------------------");
        for (final ReceiptItem item : this.state.receipt.items) {
            System.out.print(item.description);
            System.out.print("(" + item.quantity + ")=R$ ");
            System.out.println(item.value);
        }
        System.out.println("------------------------------------------------------------");
        System.out.println("TOTAL: " + this.state.receipt.total);

    }

}
