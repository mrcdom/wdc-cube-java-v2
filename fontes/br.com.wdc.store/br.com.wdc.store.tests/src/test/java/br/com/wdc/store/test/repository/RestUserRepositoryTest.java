package br.com.wdc.store.test.repository;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.TestEnvironment;

public class RestUserRepositoryTest extends AbstractUserRepositoryTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST);

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	@Override
	protected UserRepository repo() {
		return env.userRepo();
	}

	// :: Alternative groups (anyOf) do not travel

	@Test
	public void anyOf_isRefusedByTheClient_beforeTheRequest() {
		var criteria = new UserCriteria()
				.withAnyOf(
						new UserCriteria()
								.withUserName("admin"),
						new UserCriteria()
								.withUserName("jdoe"));

		var e = assertThrows(InvalidRequestException.class, () -> repo().fetch(criteria));
		assertTrue(e.getMessage(), e.getMessage().contains("anyOf"));
	}

	@Test
	public void anyOf_sentByHand_isRefusedByTheServer() {
		// Skipping the key would answer with every user — a looser filter than the one sent
		var e = assertThrows(InvalidRequestException.class, () -> env.transport().postJson("/api/repo/user/fetch",
				"{\"anyOf\":[{\"userName\":\"admin\"}]}"));
		assertTrue(e.getMessage(), e.getMessage().contains("anyOf"));
	}
}
