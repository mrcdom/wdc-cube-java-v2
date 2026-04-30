package br.com.wdc.shopping.test;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import br.com.wdc.framework.commons.util.LambdaUtils;
import br.com.wdc.shopping.persistence.sgbd.ddl.scripts.DBReset;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria.OrderBy;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginService;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeService;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductService;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptService;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptForm;
import br.com.wdc.shopping.test.util.BaseBusinessTest;

public class ShoppingServiceTest extends BaseBusinessTest {

    @Test
    public void test1() {
        var pv = ProjectionValues.INSTANCE;

        var usrPrj = new User();
        usrPrj.id = pv.i64;
        usrPrj.userName = pv.str;

        var prdPrj = new Product();
        prdPrj.id = pv.i64;
        prdPrj.name = pv.str;

        var pchPrj = new Purchase();
        pchPrj.id = pv.i64;
        pchPrj.user = usrPrj;
        pchPrj.buyDate = pv.offsetDateTime;

        var itemPrj = new PurchaseItem();
        itemPrj.id = pv.i64;
        itemPrj.amount = pv.i32;
        itemPrj.product = prdPrj;
        itemPrj.price = pv.f64;
        itemPrj.purchase = pchPrj;

        var purchaseItemList = PurchaseItemRepository.BEAN.get().fetch(new PurchaseItemCriteria()
                .withUserId(DBReset.ADMIN_ID)
                .withProjection(itemPrj));
        assertEquals("purchaseItemList.size()", 3, purchaseItemList.size());
    }

    @Test
    public void test() {
        var subject = LoginService.BEAN.fetchSubject("admin", "admin");
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

            final ProductInfo mesmoProduto = ProductService.BEAN.loadProductById(produto.id);
            Assert.assertEquals(produto.id, mesmoProduto.id);
            Assert.assertEquals(produto.name, mesmoProduto.name);
            Assert.assertEquals(produto.image, mesmoProduto.image);
            Assert.assertEquals(produto.price, mesmoProduto.price, 0.001);
            Assert.assertEquals(produto.description, mesmoProduto.description);
        }

        List<PurchaseInfo> compras = HomeService.BEAN.loadPurchases(new PurchaseCriteria()
                .withOrderBy(OrderBy.ACENDING));

        Assert.assertNotNull(compras);
        Assert.assertEquals(2, compras.size());

        Assert.assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, Long.valueOf(compras.get(0).id));
        Assert.assertNotNull(compras.get(0).items);
        Assert.assertEquals(1, compras.get(0).items.size());

        Assert.assertEquals(DBReset.ADMIN_SECOND_PURCHASE_ID, Long.valueOf(compras.get(1).id));
        Assert.assertNotNull(compras.get(1).items);
        Assert.assertEquals(2, compras.get(1).items.size());

        Purchase purchase = new Purchase();
        purchase.user = new User();
        purchase.user.id = userId;
        purchase.buyDate = OffsetDateTime.now();
        purchase.items = new ArrayList<>();
        purchase.items.add(LambdaUtils.supply(() -> {
            var item = new PurchaseItem();
            item.product = new Product();
            item.product.id = DBReset.PEN_DRIVE2GB_ID;
            item.price = 55.0;
            item.amount = 1;
            return item;
        }));
        purchase.items.add(LambdaUtils.supply(() -> {
            var item = new PurchaseItem();
            item.product = new Product();
            item.product.id = DBReset.FITA_VEDA_ROSCA_ID;
            item.price = 5.0;
            item.amount = 2;
            return item;
        }));

        PurchaseRepository.BEAN.get().insert(purchase);
        final long idCompra = purchase.id;
        Assert.assertEquals(DBReset.ADMIN_SECOND_PURCHASE_ID + 1, idCompra);

        compras = HomeService.BEAN.loadPurchasesOfUser(userId);

        Assert.assertNotNull(compras);
        Assert.assertEquals(3, compras.size());

        var ultimaCompra = compras.get(0);
        Assert.assertEquals(Long.valueOf(idCompra), Long.valueOf(ultimaCompra.id));
        Assert.assertEquals(2, ultimaCompra.items.size());
        Assert.assertEquals(Double.valueOf(60.0), Double.valueOf(ultimaCompra.total));

        final ReceiptForm recibo = ReceiptService.BEAN.loadReceipt(idCompra);
        Assert.assertNotNull(recibo);
        Assert.assertEquals(Double.valueOf(60), recibo.total);
        Assert.assertEquals(2, recibo.items.size());

        var pedido0 = purchase.items.get(0);
        Assert.assertEquals(pedido0.price, Double.valueOf(recibo.items.get(0).value));
        Assert.assertEquals(pedido0.amount, Integer.valueOf(recibo.items.get(0).quantity));
        Assert.assertEquals("Pen Drive 2GB", recibo.items.get(0).description);

        var pedido1 = purchase.items.get(1);
        Assert.assertEquals(pedido1.price, Double.valueOf(recibo.items.get(1).value));
        Assert.assertEquals(pedido1.amount, Integer.valueOf(recibo.items.get(1).quantity));
        Assert.assertEquals("Fita veda rosca", recibo.items.get(1).description);
    }

}
