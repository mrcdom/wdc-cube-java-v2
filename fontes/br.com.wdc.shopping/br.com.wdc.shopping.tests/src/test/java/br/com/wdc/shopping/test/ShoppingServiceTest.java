package br.com.wdc.shopping.test;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import br.com.wdc.framework.commons.util.LambdaUtils;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.product.ProductCriteria;
import br.com.wdc.shopping.domain.product.ProductRepository;
import br.com.wdc.shopping.domain.purchase.Purchase;
import br.com.wdc.shopping.domain.purchase.PurchaseCriteria.OrderBy;
import br.com.wdc.shopping.domain.purchase.PurchaseCriteria;
import br.com.wdc.shopping.domain.purchase.PurchaseRepository;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.shopping.domain.user.User;
import br.com.wdc.shopping.domain.user.UserRepository;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginService;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelService;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductService;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptService;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptForm;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.util.BaseBusinessTest;

@SuppressWarnings("java:S5961") // integration test — many assertions validate end-to-end flow
public class ShoppingServiceTest extends BaseBusinessTest {

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
        // A senha não é campo de critério: quem a confere é o login, e é neste nível que o par
        // "aceita a correta / recusa a errada" precisa ser garantido.
        var result = new LoginService(UserRepository.BEAN.get()).fetchSubject("admin", "senha-errada");

        Assert.assertNull("senha errada não pode autenticar", result);
    }

    @Test
    public void login_unknownUser_isRejected() {
        var result = new LoginService(UserRepository.BEAN.get()).fetchSubject("ninguem", "admin");

        Assert.assertNull("usuário inexistente não pode autenticar", result);
    }

    @Test
    public void test() {
        var result = new LoginService(UserRepository.BEAN.get()).fetchSubject("admin", "admin");
        var subject = result != null ? result.subject() : null;
        Assert.assertNotNull("Missing subject", subject);

        Assert.assertTrue("Subject.id must be a Long type", subject.getId() instanceof Long);
        var userId = subject.getId();

        Assert.assertEquals("UserId must be administrator", DBReset.ADMIN_ID, userId);

        Assert.assertEquals("User name did not match", "João da Silva", subject.getNickName());

        var produtos = ProductRepository.BEAN.get().fetch(new ProductCriteria())
                .stream().map(ProductInfo::create).toList();

        Assert.assertNotNull(produtos);
        Assert.assertEquals(4, produtos.size());
        Assert.assertEquals(DBReset.CAFETEIRA_ID, Long.valueOf(produtos.get(0).id));
        Assert.assertEquals(DBReset.BOLA_WILSON_ID, Long.valueOf(produtos.get(1).id));
        Assert.assertEquals(DBReset.FITA_VEDA_ROSCA_ID, Long.valueOf(produtos.get(2).id));
        Assert.assertEquals(DBReset.PEN_DRIVE2GB_ID, Long.valueOf(produtos.get(3).id));

        for (final ProductInfo produto : produtos) {
            Assert.assertTrue("Product name can not be empty", StringUtils.isNotBlank(produto.name));
            Assert.assertTrue("Product image name can not end differently than .png", produto.image.endsWith(".png"));
            Assert.assertTrue("Product price must be grater than or equal to 0.0", produto.price >= 0.0);
            Assert.assertTrue("Product description can not be empty", StringUtils.isNotBlank(produto.description));

            final ProductInfo mesmoProduto = new ProductService(ProductRepository.BEAN.get()).loadProductById(produto.id);
            Assert.assertEquals(produto.id, mesmoProduto.id);
            Assert.assertEquals(produto.name, mesmoProduto.name);
            Assert.assertEquals(produto.image, mesmoProduto.image);
            Assert.assertEquals(produto.price, mesmoProduto.price, 0.001);
            Assert.assertEquals(produto.description, mesmoProduto.description);
        }

        var homeService = new PurchasesPanelService(PurchaseRepository.BEAN.get());

        List<PurchaseInfo> compras = homeService.loadPurchases(new PurchaseCriteria()
                .withOrderBy(OrderBy.OLDEST_FIRST));

        Assert.assertNotNull(compras);
        Assert.assertEquals(2, compras.size());

        Assert.assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, Long.valueOf(compras.get(0).id));
        Assert.assertNotNull(compras.get(0).items);
        Assert.assertEquals(1, compras.get(0).items.size());

        Assert.assertEquals(DBReset.ADMIN_SECOND_PURCHASE_ID, Long.valueOf(compras.get(1).id));
        Assert.assertNotNull(compras.get(1).items);
        Assert.assertEquals(2, compras.get(1).items.size());

        Purchase purchase = new Purchase()
                .withUser(new User().withId(userId));
        purchase.withBuyDate(OffsetDateTime.now())
                .withItems(new ArrayList<>());
        purchase.items().add(LambdaUtils.supply(() -> {
            var item = new PurchaseItem()
                    .withProduct(new Product().withId(DBReset.PEN_DRIVE2GB_ID));
            item.withPrice(55.0)
                    .withAmount(1);
            return item;
        }));
        purchase.items().add(LambdaUtils.supply(() -> {
            var item = new PurchaseItem()
                    .withProduct(new Product().withId(DBReset.FITA_VEDA_ROSCA_ID));
            item.withPrice(5.0)
                    .withAmount(2);
            return item;
        }));

        PurchaseRepository.BEAN.get().insert(purchase);
        final long idCompra = purchase.id();
        Assert.assertEquals(DBReset.ADMIN_SECOND_PURCHASE_ID + 1, idCompra);

        compras = homeService.loadPurchasesOfUser(userId);

        Assert.assertNotNull(compras);
        Assert.assertEquals(3, compras.size());

        var ultimaCompra = compras.get(0);
        Assert.assertEquals(Long.valueOf(idCompra), Long.valueOf(ultimaCompra.id));
        Assert.assertEquals(2, ultimaCompra.items.size());
        Assert.assertEquals(Double.valueOf(65.0), Double.valueOf(ultimaCompra.total));

        final ReceiptForm recibo = new ReceiptService(PurchaseRepository.BEAN.get()).loadReceipt(idCompra);
        Assert.assertNotNull(recibo);
        Assert.assertEquals(Double.valueOf(65), recibo.total);
        Assert.assertEquals(2, recibo.items.size());

        var pedido0 = purchase.items().get(0);
        Assert.assertEquals(pedido0.price(), Double.valueOf(recibo.items.get(0).value));
        Assert.assertEquals(pedido0.amount(), Integer.valueOf(recibo.items.get(0).quantity));
        Assert.assertEquals("Pen Drive 2GB", recibo.items.get(0).description);

        var pedido1 = purchase.items().get(1);
        Assert.assertEquals(pedido1.price(), Double.valueOf(recibo.items.get(1).value));
        Assert.assertEquals(pedido1.amount(), Integer.valueOf(recibo.items.get(1).quantity));
        Assert.assertEquals("Fita veda rosca", recibo.items.get(1).description);
    }

}
