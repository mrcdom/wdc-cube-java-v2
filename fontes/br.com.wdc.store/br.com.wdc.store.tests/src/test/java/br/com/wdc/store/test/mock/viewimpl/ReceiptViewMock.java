package br.com.wdc.store.test.mock.viewimpl;

import java.util.Date;

import org.junit.Assert;

import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptScope;
import br.com.wdc.store.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.store.test.mock.StoreApplicationMock;

@SuppressWarnings({ "java:S106", "java:S1192" })
public class ReceiptViewMock extends AbstractViewMock<ReceiptScope> {

    public static ReceiptViewMock cast(Object view) {
        var cls = ReceiptViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (ReceiptViewMock) view;
    }

    public ReceiptViewMock(StoreApplicationMock app, ReceiptScope scope) {
        super(app, scope);
    }

    public void printReceipt() {
        if (this.scope.notifySuccess.get()) {
            System.out.println("Purchase completed successfully");
            System.out.println();
        }

        System.out.println("Print your receipt:");

        System.out.println("------------------------------------------------------------");
        System.out.println("WDC Store - THE RIGHT WAY TO SHOP ONLINE");
        System.out.println("Purchase receipt");
        System.out.println("Date: " + new Date(this.scope.receipt.get().date));
        System.out.println("------------------------------------------------------------");
        for (final ReceiptItem item : this.scope.receipt.get().items) {
            System.out.print(item.description);
            System.out.print("(" + item.quantity + ")=R$ ");
            System.out.println(item.value);
        }
        System.out.println("------------------------------------------------------------");
        System.out.println("TOTAL: " + this.scope.receipt.get().total);

    }

}
