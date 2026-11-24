package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.security.PasswordUtil;
import br.com.wdc.store.domain.product.Product;
import br.com.wdc.store.domain.product.ProductCriteria;
import br.com.wdc.store.domain.purchase.Purchase;
import br.com.wdc.store.domain.purchase.PurchaseCriteria;
import br.com.wdc.store.domain.purchaseitem.PurchaseItem;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.RestSession;
import br.com.wdc.store.test.util.TestEnvironment;

/**
 * Purchases, purchase items and products over the REST API with authentication on: what each role reaches through
 * {@code PurchaseDataPolicy}, {@code PurchaseItemDataPolicy} and {@code ProductDataPolicy}, and the relations between
 * them — a purchase's user goes through the user policy, its items through the item policy.
 *
 * <p>
 * The seed's purchases are the ADMIN's; each test has {@code jdoe} (CUSTOMER) buy a coffee maker first, through the
 * same insert the checkout uses, and adds a MANAGER ({@code mmajor}).
 * </p>
 */
public class PurchaseDataPolicyRestTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST).withSecurity();

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	private static final ProjectionValues PV = ProjectionValues.INSTANCE;

	private RestSession jdoe;
	private Long jdoePurchaseId;

	@Before
	public void jdoeBuysACoffeeMaker() {
		UserRepository.BEAN.get().insert(new User()
				.withUserName("mmajor")
				.withPassword(PasswordUtil.hashPassword("mmajor"))
				.withName("Mary Major")
				.withRoles("MANAGER"));

		jdoe = RestSession.signIn(env, "jdoe");
		var purchase = newPurchase(DBReset.JOHN_DOE_ID, new PurchaseItem()
				.withProduct(new Product()
						.withId(DBReset.COFFEE_MAKER_ID))
				.withAmount(1));
		assertTrue(jdoe.purchases().insert(purchase));
		jdoePurchaseId = purchase.id();
	}

	// :: Creating

	@Test
	public void theItemPrice_isThePriceOfItsProduct() {
		var admin = RestSession.signIn(env, "admin");

		var items = admin.items().fetch(new PurchaseItemCriteria()
				.withPurchaseId(jdoePurchaseId)
				.withProjection(new PurchaseItem()
						.withPrice(PV.f64)));

		assertEquals(1, items.size());
		assertEquals("the coffee maker's price, not one sent by the client", 199.99, items.get(0).price(), 0.001);
	}

	@Test
	public void customer_cannotSendThePrice() {
		var e = assertThrows(AccessDeniedException.class, () -> jdoe.purchases().insert(newPurchase(DBReset.JOHN_DOE_ID,
				new PurchaseItem()
						.withProduct(new Product()
								.withId(DBReset.COFFEE_MAKER_ID))
						.withAmount(1)
						.withPrice(0.01))));

		assertTrue(e.getMessage(), e.getMessage().contains("price"));
	}

	@Test
	public void customer_cannotBuyForSomeoneElse() {
		assertThrows(AccessDeniedException.class, () -> jdoe.purchases().insert(newPurchase(DBReset.JANE_ROE_ID,
				new PurchaseItem()
						.withProduct(new Product()
								.withId(DBReset.COFFEE_MAKER_ID))
						.withAmount(1))));
	}

	@Test
	public void customer_addsItemsOnlyToTheirOwnPurchases() {
		var mine = new PurchaseItem()
				.withPurchase(new Purchase()
						.withId(jdoePurchaseId))
				.withProduct(new Product()
						.withId(DBReset.WILSON_BALL_ID))
				.withAmount(2);
		assertTrue(jdoe.items().insert(mine));

		var intoTheAdmins = new PurchaseItem()
				.withPurchase(new Purchase()
						.withId(DBReset.ADMIN_FIRST_PURCHASE_ID))
				.withProduct(new Product()
						.withId(DBReset.WILSON_BALL_ID))
				.withAmount(2);
		assertThrows(AccessDeniedException.class, () -> jdoe.items().insert(intoTheAdmins));
	}

	// :: Reading

	@Test
	public void customer_readsOnlyTheirOwnPurchases() {
		var purchases = jdoe.purchases().fetch(new PurchaseCriteria()
				.withProjection(new Purchase()
						.withBuyDate(PV.offsetDateTime)));

		assertEquals(1, purchases.size());
		assertEquals(jdoePurchaseId, purchases.get(0).id());
		assertEquals(1, jdoe.purchases().count(new PurchaseCriteria()));
	}

	@Test
	public void customer_fetchByIdOfAnotherPurchase_findsNothing_evenWithoutTheUserInTheProjection() {
		// The projection leaves the user out: the row check must not depend on what the client asked for
		assertNull(jdoe.transport().postJsonNullable("/api/repo/purchase/fetch-by-id",
				"{\"id\":" + DBReset.ADMIN_FIRST_PURCHASE_ID + ",\"projection\":{\"id\":1,\"buyDate\":\"2000-01-01T00:00:00Z\"}}"));
	}

	@Test
	public void theUserOfAPurchase_goesThroughTheUserPolicy() {
		var admin = RestSession.signIn(env, "admin");

		var e = assertThrows(AccessDeniedException.class, () -> admin.purchases().fetch(new PurchaseCriteria()
				.withProjection(new Purchase()
						.withUser(new User()
								.withName(PV.str)
								.withPassword(PV.str)))));
		assertTrue("not even an ADMIN reads a password: " + e.getMessage(), e.getMessage().contains("[password]"));

		var own = jdoe.purchases().fetchById(jdoePurchaseId, new Purchase()
				.withUser(new User()
						.withName(PV.str)));
		assertEquals("John Doe", own.user().name());
	}

	@Test
	public void theItemsOfAPurchase_goThroughTheItemPolicy() {
		var item = new PurchaseItem()
				.withAmount(PV.i32)
				.withPrice(PV.f64)
				.withProduct(new Product()
						.withName(PV.str));

		var purchase = jdoe.purchases().fetchById(jdoePurchaseId, new Purchase()
				.withBuyDate(PV.offsetDateTime)
				.withItems(PV.singletonList(item, new PurchaseItemCriteria())));

		assertEquals(1, purchase.items().size());
		assertEquals("Italian Design Coffee Maker", purchase.items().get(0).product().name());
		// The codec rebuilds the item's purchase as a key-only back-reference; the user under it, fetched for the row
		// check, does not come back
		assertNull(purchase.items().get(0).purchase().user());
	}

	@Test
	public void customer_readsOnlyTheItemsOfTheirOwnPurchases() {
		var page = jdoe.items().fetchPage(new PurchaseItemCriteria()
				.withProjection(new PurchaseItem()
						.withAmount(PV.i32)), 0, 10);

		assertEquals(1, page.totalItems());
		assertNull(jdoe.transport().postJsonNullable("/api/repo/purchase-item/fetch-by-id",
				"{\"id\":" + DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID + "}"));
	}

	@Test
	public void manager_readsOnlyTheirOwnPurchases() {
		var manager = RestSession.signIn(env, "mmajor");

		assertTrue(manager.purchases().fetch(new PurchaseCriteria()).isEmpty());
		assertThrows(AccessDeniedException.class, () -> manager.purchases().insert(newPurchase(DBReset.JOHN_DOE_ID,
				new PurchaseItem()
						.withProduct(new Product()
								.withId(DBReset.COFFEE_MAKER_ID))
						.withAmount(1))));
	}

	// :: A purchase is a receipt

	@Test
	public void customer_cannotChangeOrDeleteAPurchase() {
		assertThrows(AccessDeniedException.class, () -> jdoe.purchases().update(new Purchase()
				.withId(jdoePurchaseId)
				.withBuyDate(OffsetDateTime.now()), new Purchase()
						.withId(jdoePurchaseId)));
		assertThrows(AccessDeniedException.class, () -> jdoe.purchases().delete(new PurchaseCriteria()
				.withPurchaseId(jdoePurchaseId)));
		assertThrows(AccessDeniedException.class, () -> jdoe.items().update(new PurchaseItem()
				.withId(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID)
				.withAmount(99), new PurchaseItem()
						.withId(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID)));
	}

	@Test
	public void admin_changesAndDeletesPurchases() {
		var admin = RestSession.signIn(env, "admin");

		assertTrue(admin.purchases().update(new Purchase()
				.withId(jdoePurchaseId)
				.withBuyDate(OffsetDateTime.parse("2020-01-01T10:00:00Z")), new Purchase()
						.withId(jdoePurchaseId)));
		assertEquals(1, admin.purchases().delete(new PurchaseCriteria()
				.withPurchaseId(jdoePurchaseId)));
	}

	// :: Products

	@Test
	public void everyRoleReadsTheCatalog_onlyAdminAndManagerChangeIt() {
		assertEquals(4, jdoe.products().fetch(new ProductCriteria()).size());

		assertThrows(AccessDeniedException.class, () -> jdoe.products().update(new Product()
				.withId(DBReset.COFFEE_MAKER_ID)
				.withPrice(1.0), new Product()
						.withId(DBReset.COFFEE_MAKER_ID)));
		assertThrows(AccessDeniedException.class,
				() -> jdoe.products().updateImage(DBReset.COFFEE_MAKER_ID, new byte[] { 1, 2, 3 }));

		var manager = RestSession.signIn(env, "mmajor");
		assertTrue(manager.products().update(new Product()
				.withId(DBReset.COFFEE_MAKER_ID)
				.withPrice(189.99), new Product()
						.withId(DBReset.COFFEE_MAKER_ID)));
		assertNotNull(manager.products().fetchById(DBReset.COFFEE_MAKER_ID, new Product()
				.withPrice(PV.f64)));
		assertFalse(manager.products().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.CHEAPEST_FIRST)).isEmpty());
	}

	// :: Helpers

	private static Purchase newPurchase(Long userId, PurchaseItem... items) {
		return new Purchase()
				.withUser(new User()
						.withId(userId))
				.withBuyDate(OffsetDateTime.now())
				.withItems(new ArrayList<>(List.of(items)));
	}
}
