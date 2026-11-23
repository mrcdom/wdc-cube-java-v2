package br.com.wdc.shopping.test;

import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.util.ResetDatabaseRule;
import br.com.wdc.shopping.test.util.TestEnvironment;

/**
 * The two lookup-by-key endpoints, called directly.
 *
 * <p>
 * They serve external consumers of the API: this project's HTTP client goes through {@code /fetch}, building the
 * criteria. These are therefore the only tests that exercise them — without them, a breakage here goes unnoticed.
 * </p>
 */
public class FetchByIdEndpointTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST);

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	@Test
	public void getById_returnsTheEntity() {
		var json = env.transport().getJson("/api/repo/product/" + DBReset.COFFEE_MAKER_ID);

		assertNotNull(json);
		assertTrue("must return the requested id: " + json, json.contains("\"id\":" + DBReset.COFFEE_MAKER_ID));
		assertTrue("must return the name: " + json, json.contains("Coffee Maker"));
	}

	@Test
	public void getById_nonExistent_answers404() {
		var e = assertThrows(BusinessException.class,
				() -> env.transport().getJson("/api/repo/product/" + Long.MAX_VALUE));

		assertTrue("a nonexistent key must be 404: " + e.getMessage(), e.getMessage().startsWith("HTTP 404"));
	}

	@Test
	public void postFetchById_honoursTheProjection() {
		var json = env.transport().postJsonNullable("/api/repo/product/fetch-by-id",
				"{\"id\":" + DBReset.COFFEE_MAKER_ID + ",\"projection\":{\"id\":1,\"name\":\"~\"}}");

		assertNotNull(json);
		assertTrue("must return the projected name: " + json, json.contains("Coffee Maker"));
		assertFalse("must not return a field outside the projection: " + json, json.contains("\"price\""));
	}

	@Test
	public void postFetchById_nonExistent_returnsNothing() {
		var json = env.transport().postJsonNullable("/api/repo/product/fetch-by-id",
				"{\"id\":" + Long.MAX_VALUE + "}");

		assertNull("a nonexistent key must not return a body", json);
	}
}
