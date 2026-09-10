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
 * Os dois endpoints de busca por chave, chamados diretamente.
 *
 * <p>
 * Eles servem quem consome a API de fora: o cliente HTTP deste projeto vai por {@code /fetch}, montando o critério.
 * São, portanto, os únicos testes que os exercitam — sem eles, uma quebra aqui passa despercebida.
 * </p>
 */
public class FetchByIdEndpointTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST);

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	@Test
	public void getById_returnsTheEntity() {
		var json = env.transport().getJson("/api/repo/product/" + DBReset.CAFETEIRA_ID);

		assertNotNull(json);
		assertTrue("deve trazer o id pedido: " + json, json.contains("\"id\":" + DBReset.CAFETEIRA_ID));
		assertTrue("deve trazer o nome: " + json, json.contains("Cafeteira"));
	}

	@Test
	public void getById_nonExistent_answers404() {
		var e = assertThrows(BusinessException.class,
				() -> env.transport().getJson("/api/repo/product/" + Long.MAX_VALUE));

		assertTrue("chave inexistente deve ser 404: " + e.getMessage(), e.getMessage().startsWith("HTTP 404"));
	}

	@Test
	public void postFetchById_honoursTheProjection() {
		var json = env.transport().postJsonNullable("/api/repo/product/fetch-by-id",
				"{\"id\":" + DBReset.CAFETEIRA_ID + ",\"projection\":{\"id\":1,\"name\":\"~\"}}");

		assertNotNull(json);
		assertTrue("deve trazer o nome projetado: " + json, json.contains("Cafeteira"));
		assertFalse("não deve trazer campo fora da projeção: " + json, json.contains("\"price\""));
	}

	@Test
	public void postFetchById_nonExistent_returnsNothing() {
		var json = env.transport().postJsonNullable("/api/repo/product/fetch-by-id",
				"{\"id\":" + Long.MAX_VALUE + "}");

		assertNull("chave inexistente não deve devolver corpo", json);
	}
}
