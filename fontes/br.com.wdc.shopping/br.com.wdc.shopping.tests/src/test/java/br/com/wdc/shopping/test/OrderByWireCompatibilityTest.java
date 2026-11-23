package br.com.wdc.shopping.test;

import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Test;

import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.shopping.domain.product.ProductCodec;
import br.com.wdc.shopping.domain.product.ProductCriteria;
import br.com.wdc.shopping.domain.purchase.PurchaseCodec;
import br.com.wdc.shopping.domain.purchase.PurchaseCriteria;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCodec;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.user.UserCodec;
import br.com.wdc.shopping.domain.user.UserCriteria;
import br.com.wdc.shopping.test.util.TestEnvironment;

/**
 * What happens when an ordering name this server does not know arrives.
 *
 * <p>
 * That is what an outdated client sends — the app compiled against an earlier version of the domain, which keeps
 * asking for a name that has since been renamed. The response must name the rejected value and the accepted ones, and
 * arrive as a 400: it is the caller's only clue, since for them the symptom is an empty list on screen.
 * </p>
 */
public class OrderByWireCompatibilityTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST);

	@Test
	public void unknownOrderBy_isRejectedNamingValueAndAccepted() {
		var in = new JsonStreamReader("\"NONEXISTENT_ORDERING\"");
		var criteria = new PurchaseCriteria();

		var e = assertThrows(InvalidRequestException.class,
				() -> new PurchaseCodec().readCriteriaField(in, "orderBy", criteria));

		assertTrue("the message must name the rejected value: " + e.getMessage(),
				e.getMessage().contains("NONEXISTENT_ORDERING"));
		assertTrue("the message must list the accepted orderings: " + e.getMessage(),
				e.getMessage().contains("NEWEST_FIRST"));
	}

	@Test
	public void unknownOrderBy_isRejectedInEveryEntity() {
		assertThrows(InvalidRequestException.class,
				() -> new ProductCodec().readCriteriaField(new JsonStreamReader("\"NONEXISTENT_ORDERING\""), "orderBy",
						new ProductCriteria()));
		assertThrows(InvalidRequestException.class,
				() -> new UserCodec().readCriteriaField(new JsonStreamReader("\"NONEXISTENT_ORDERING\""), "orderBy",
						new UserCriteria()));
		assertThrows(InvalidRequestException.class,
				() -> new PurchaseItemCodec().readCriteriaField(new JsonStreamReader("\"NONEXISTENT_ORDERING\""), "orderBy",
						new PurchaseItemCriteria()));
	}

	@Test
	public void knownOrderBy_isRead() {
		var criteria = new PurchaseCriteria();
		new PurchaseCodec().readCriteriaField(new JsonStreamReader("\"NEWEST_FIRST\""), "orderBy", criteria);
		assertEquals(PurchaseCriteria.OrderBy.NEWEST_FIRST, criteria.orderBy());

		var byDate = new PurchaseCriteria();
		new PurchaseCodec().readCriteriaField(new JsonStreamReader("\"MOST_RECENT_PURCHASE_FIRST\""), "orderBy", byDate);
		assertEquals(PurchaseCriteria.OrderBy.MOST_RECENT_PURCHASE_FIRST, byDate.orderBy());
	}

	@Test
	public void absentOrderBy_leavesCriteriaUntouched() {
		var criteria = new PurchaseCriteria()
				.withOrderBy(PurchaseCriteria.OrderBy.OLDEST_FIRST);
		new PurchaseCodec().readCriteriaField(new JsonStreamReader("null"), "orderBy", criteria);
		assertEquals("null is not an ordering: it must not erase the existing one",
				PurchaseCriteria.OrderBy.OLDEST_FIRST, criteria.orderBy());
	}

	/** Round trip through the wire format: what the writer produces, the reader accepts. */
	@Test
	public void everyOrderingSurvivesTheRoundTrip() {
		for (var ordering : PurchaseCriteria.OrderBy.values()) {
			var criteria = new PurchaseCriteria();
			new PurchaseCodec().readCriteriaField(new JsonStreamReader("\"" + ordering.name() + "\""), "orderBy",
					criteria);
			assertEquals(ordering, criteria.orderBy());
		}
	}

	/**
	 * The whole path, over HTTP.
	 *
	 * <p>
	 * What matters here is the <b>status</b>: the exception has to cross the Javalin handler and come out as a 400, with
	 * the message in the body. Mapped as an internal failure, the client loses the only clue it has.
	 * </p>
	 */
	@Test
	public void unknownOrderBy_overHttp_answers400WithTheReason() {
		var body = "{\"userId\":{\"p\":[{\"o\":\"EQ\",\"v\":[0]}]},\"orderBy\":\"NONEXISTENT_ORDERING\","
				+ "\"projection\":{\"id\":1},\"pageSize\":10}";

		// 400, not 500: it comes back as the exception the server threw
		var e = assertThrows(InvalidRequestException.class,
				() -> env.transport().postJson("/api/repo/purchase/fetch-page", body));

		assertTrue("the body must name the rejected value: " + e.getMessage(),
				e.getMessage().contains("NONEXISTENT_ORDERING"));
		assertTrue("the body must list the accepted ones: " + e.getMessage(),
				e.getMessage().contains("NEWEST_FIRST"));
	}
}
