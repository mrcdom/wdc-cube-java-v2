package br.com.wdc.shopping.test;

import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Test;

import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.domain.exception.BusinessException;
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
 * O que acontece quando chega um nome de ordenação que este servidor não conhece.
 *
 * <p>
 * É o que um cliente defasado envia — o app compilado contra uma versão anterior do domínio, que segue pedindo um
 * nome já renomeado. A resposta tem de nomear o valor recusado e os aceitos, e chegar como 400: é a única pista de
 * quem chamou, para quem o sintoma é uma lista vazia na tela.
 * </p>
 */
public class OrderByWireCompatibilityTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST);

	@Test
	public void unknownOrderBy_isRejectedNamingValueAndAccepted() {
		var in = new JsonStreamReader("\"ORDENACAO_INEXISTENTE\"");
		var criteria = new PurchaseCriteria();

		var e = assertThrows(InvalidRequestException.class,
				() -> new PurchaseCodec().readCriteriaField(in, "orderBy", criteria));

		assertTrue("a mensagem deve nomear o valor recusado: " + e.getMessage(),
				e.getMessage().contains("ORDENACAO_INEXISTENTE"));
		assertTrue("a mensagem deve listar as ordenações aceitas: " + e.getMessage(),
				e.getMessage().contains("NEWEST_FIRST"));
	}

	@Test
	public void unknownOrderBy_isRejectedInEveryEntity() {
		assertThrows(InvalidRequestException.class,
				() -> new ProductCodec().readCriteriaField(new JsonStreamReader("\"ORDENACAO_INEXISTENTE\""), "orderBy",
						new ProductCriteria()));
		assertThrows(InvalidRequestException.class,
				() -> new UserCodec().readCriteriaField(new JsonStreamReader("\"ORDENACAO_INEXISTENTE\""), "orderBy",
						new UserCriteria()));
		assertThrows(InvalidRequestException.class,
				() -> new PurchaseItemCodec().readCriteriaField(new JsonStreamReader("\"ORDENACAO_INEXISTENTE\""), "orderBy",
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
		var criteria = new PurchaseCriteria().withOrderBy(PurchaseCriteria.OrderBy.OLDEST_FIRST);
		new PurchaseCodec().readCriteriaField(new JsonStreamReader("null"), "orderBy", criteria);
		assertEquals("null não é ordenação: não deve apagar a que já estava",
				PurchaseCriteria.OrderBy.OLDEST_FIRST, criteria.orderBy());
	}

	/** Ida e volta pelo formato do fio: o que a escrita produz, a leitura aceita. */
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
	 * O caminho inteiro, pelo HTTP.
	 *
	 * <p>
	 * O que importa aqui é o <b>status</b>: a exceção precisa atravessar o handler do Javalin e sair como 400, com a
	 * mensagem no corpo. Mapeada como falha interna, o cliente fica sem a única pista que tem.
	 * </p>
	 */
	@Test
	public void unknownOrderBy_overHttp_answers400WithTheReason() {
		var body = "{\"userId\":{\"p\":[{\"o\":\"EQ\",\"v\":[0]}]},\"orderBy\":\"ORDENACAO_INEXISTENTE\","
				+ "\"projection\":{\"id\":1},\"pageSize\":10}";

		var e = assertThrows(BusinessException.class,
				() -> env.transport().postJson("/api/repo/purchase/fetch-page", body));

		assertTrue("deve ser 400, não 500: " + e.getMessage(), e.getMessage().startsWith("HTTP 400"));
		assertTrue("o corpo deve nomear o valor recusado: " + e.getMessage(),
				e.getMessage().contains("ORDENACAO_INEXISTENTE"));
		assertTrue("o corpo deve listar as aceitas: " + e.getMessage(),
				e.getMessage().contains("NEWEST_FIRST"));
	}
}
