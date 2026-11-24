package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import br.com.wdc.framework.commons.util.LambdaUtils;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.store.domain.product.Product;
import br.com.wdc.store.domain.product.ProductCriteria;
import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.domain.purchase.Purchase;
import br.com.wdc.store.domain.purchase.PurchaseCriteria;
import br.com.wdc.store.domain.purchase.PurchaseRepository;
import br.com.wdc.store.domain.purchaseitem.PurchaseItem;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.presentation.StoreSave;
import br.com.wdc.store.presentation.presenter.open.login.calls.LoginCall;
import br.com.wdc.store.presentation.presenter.restricted.home.calls.PurchasesPageQuery;
import br.com.wdc.store.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.store.presentation.presenter.restricted.products.calls.ProductQuery;
import br.com.wdc.store.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.store.presentation.presenter.restricted.receipt.calls.ReceiptQuery;
import br.com.wdc.store.presentation.presenter.restricted.receipt.structs.ReceiptForm;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.util.BaseBusinessTest;

@SuppressWarnings("java:S5961") // integration test — many assertions validate end-to-end flow
public class StoreCallTest extends BaseBusinessTest {


    /**
     * The statement is ordered by purchase date, not by insertion order.
     *
     * <p>
     * The two only diverge when a purchase is entered with a date earlier than one already recorded — the case this
     * test builds. Ordered by key, the newly inserted purchase would head the list even though it happened two years
     * earlier.
     * </p>
     */
    @Test
    public void purchaseStatement_ordersByPurchaseDate_notByInsertionOrder() {
        // Last one inserted (highest id), yet the oldest of all.
        var backdated = new Purchase()
                .withUser(new User()
                        .withId(DBReset.ADMIN_ID))
                .withBuyDate(OffsetDateTime.of(2009, 5, 20, 10, 0, 0, 0, java.time.ZoneOffset.UTC))
                .withItems(new ArrayList<>());
        backdated.items().add(new PurchaseItem()
                .withProduct(new Product()
                        .withId(DBReset.FLASH_DRIVE_2GB_ID))
                .withPrice(55.0)
                .withAmount(1));
        PurchaseRepository.BEAN.get().insert(backdated);

        var statement = purchasesOf(DBReset.ADMIN_ID);

        Assert.assertEquals("the 2009 purchase has the highest id", Long.valueOf(backdated.id()),
                Long.valueOf(statement.stream().mapToLong(p -> p.id).max().orElse(-1)));
        Assert.assertNotEquals("the oldest one cannot head the statement", Long.valueOf(backdated.id()),
                Long.valueOf(statement.get(0).id));
        Assert.assertEquals("the oldest one closes the list", Long.valueOf(backdated.id()),
                Long.valueOf(statement.get(statement.size() - 1).id));
    }

    @Test
    public void test1() {
        var pv = ProjectionValues.INSTANCE;

        var usrPrj = new User()
                .withId(pv.i64)
                .withUserName(pv.str);

        var prdPrj = new Product()
                .withId(pv.i64)
                .withName(pv.str);

        var pchPrj = new Purchase()
                .withId(pv.i64)
                .withUser(usrPrj)
                .withBuyDate(pv.offsetDateTime);

        var itemPrj = new PurchaseItem()
                .withId(pv.i64)
                .withAmount(pv.i32)
                .withProduct(prdPrj)
                .withPrice(pv.f64)
                .withPurchase(pchPrj);

        var purchaseItemList = PurchaseItemRepository.BEAN.get().fetch(new PurchaseItemCriteria()
                .withUserId(DBReset.ADMIN_ID)
                .withProjection(itemPrj));
        assertEquals("purchaseItemList.size()", 3, purchaseItemList.size());
    }

    @Test
    public void login_wrongPassword_isRejected() {
        // The password is not a criteria field: the login checks it, and this is the level where the pair
        // "accepts the right one / rejects the wrong one" must be guaranteed.
        var result = new LoginCall()
                .withUserName("admin")
                .withPassword("wrong-password")
                .run();

        Assert.assertNull("a wrong password must not authenticate", result);
    }

    @Test
    public void login_unknownUser_isRejected() {
        var result = new LoginCall()
                .withUserName("nobody")
                .withPassword("admin")
                .run();

        Assert.assertNull("a nonexistent user must not authenticate", result);
    }

    @Test
    public void test() {
        var result = new LoginCall()
                .withUserName("admin")
                .withPassword("admin")
                .run();
        var subject = result != null ? result.subject() : null;
        Assert.assertNotNull("Missing subject", subject);

        Assert.assertTrue("Subject.id must be a Long type", subject.getId() instanceof Long);
        var userId = subject.getId();

        Assert.assertEquals("UserId must be administrator", DBReset.ADMIN_ID, userId);

        Assert.assertEquals("User name did not match", "John Smith", subject.getNickName());

        var products = ProductRepository.BEAN.get().fetch(new ProductCriteria())
                .stream().map(ProductInfo::create).toList();

        Assert.assertNotNull(products);
        Assert.assertEquals(4, products.size());
        Assert.assertEquals(DBReset.COFFEE_MAKER_ID, Long.valueOf(products.get(0).id));
        Assert.assertEquals(DBReset.WILSON_BALL_ID, Long.valueOf(products.get(1).id));
        Assert.assertEquals(DBReset.THREAD_SEAL_TAPE_ID, Long.valueOf(products.get(2).id));
        Assert.assertEquals(DBReset.FLASH_DRIVE_2GB_ID, Long.valueOf(products.get(3).id));

        for (final ProductInfo product : products) {
            Assert.assertTrue("Product name can not be empty", StringUtils.isNotBlank(product.name));
            Assert.assertTrue("Product image name can not end differently than .png", product.image.endsWith(".png"));
            Assert.assertTrue("Product price must be grater than or equal to 0.0", product.price >= 0.0);
            Assert.assertTrue("Product description can not be empty", StringUtils.isNotBlank(product.description));

            final ProductInfo sameProduct = new ProductQuery()
                    .withProductId(product.id)
                    .run();
            Assert.assertEquals(product.id, sameProduct.id);
            Assert.assertEquals(product.name, sameProduct.name);
            Assert.assertEquals(product.image, sameProduct.image);
            Assert.assertEquals(product.price, sameProduct.price, 0.001);
            Assert.assertEquals(product.description, sameProduct.description);
        }

        // Most recent first
        List<PurchaseInfo> purchases = purchasesOf(userId);

        Assert.assertNotNull(purchases);
        Assert.assertEquals(2, purchases.size());

        Assert.assertEquals(DBReset.ADMIN_SECOND_PURCHASE_ID, Long.valueOf(purchases.get(0).id));
        Assert.assertNotNull(purchases.get(0).items);
        Assert.assertEquals(2, purchases.get(0).items.size());

        Assert.assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, Long.valueOf(purchases.get(1).id));
        Assert.assertNotNull(purchases.get(1).items);
        Assert.assertEquals(1, purchases.get(1).items.size());

        Purchase purchase = new Purchase()
                .withUser(new User()
                        .withId(userId));
        purchase.withBuyDate(OffsetDateTime.now())
                .withItems(new ArrayList<>());
        purchase.items().add(LambdaUtils.supply(() -> {
            var item = new PurchaseItem()
                    .withProduct(new Product()
                            .withId(DBReset.FLASH_DRIVE_2GB_ID));
            item.withPrice(55.0)
                    .withAmount(1);
            return item;
        }));
        purchase.items().add(LambdaUtils.supply(() -> {
            var item = new PurchaseItem()
                    .withProduct(new Product()
                            .withId(DBReset.THREAD_SEAL_TAPE_ID));
            item.withPrice(5.0)
                    .withAmount(2);
            return item;
        }));

        PurchaseRepository.BEAN.get().insert(purchase);
        final long purchaseId = purchase.id();
        Assert.assertEquals(DBReset.ADMIN_SECOND_PURCHASE_ID + 1, purchaseId);

        purchases = purchasesOf(userId);

        Assert.assertNotNull(purchases);
        Assert.assertEquals(3, purchases.size());

        var latestPurchase = purchases.get(0);
        Assert.assertEquals(Long.valueOf(purchaseId), Long.valueOf(latestPurchase.id));
        Assert.assertEquals(2, latestPurchase.items.size());
        Assert.assertEquals(Double.valueOf(65.0), Double.valueOf(latestPurchase.total));

        final ReceiptForm receipt = new ReceiptQuery()
                .withPurchaseId(purchaseId)
                .run();
        Assert.assertNotNull(receipt);
        Assert.assertEquals(Double.valueOf(65), receipt.total);
        Assert.assertEquals(2, receipt.items.size());

        var item0 = purchase.items().get(0);
        Assert.assertEquals(item0.price(), Double.valueOf(receipt.items.get(0).value));
        Assert.assertEquals(item0.amount(), Integer.valueOf(receipt.items.get(0).quantity));
        Assert.assertEquals("2GB Flash Drive", receipt.items.get(0).description);

        var item1 = purchase.items().get(1);
        Assert.assertEquals(item1.price(), Double.valueOf(receipt.items.get(1).value));
        Assert.assertEquals(item1.amount(), Integer.valueOf(receipt.items.get(1).quantity));
        Assert.assertEquals("Thread Seal Tape", receipt.items.get(1).description);
    }

    @Test
    public void aSaveThatFails_leavesNothingBehind() {
        var before = PurchaseRepository.BEAN.get().count(new PurchaseCriteria());

        // Writes and only then fails: the transaction the StoreSave opened is rolled back
        var failingSave = new StoreSave<Long>() {
            @Override
            protected Long save() {
                purchaseRepository().insert(new Purchase()
                        .withUser(new User()
                                .withId(DBReset.ADMIN_ID))
                        .withBuyDate(OffsetDateTime.now())
                        .withItems(new ArrayList<>()));
                throw new IllegalStateException("failure after writing");
            }
        };

        Assert.assertThrows(IllegalStateException.class, failingSave::run);
        Assert.assertEquals("nothing written", before, PurchaseRepository.BEAN.get().count(new PurchaseCriteria()));
    }

    /** All of the user's purchases, most recent first: one page large enough to hold them. */
    private static List<PurchaseInfo> purchasesOf(Long userId) {
        return new PurchasesPageQuery()
                .withUserId(userId)
                .withPage(0)
                .withPageSize(100)
                .run().items();
    }

}
