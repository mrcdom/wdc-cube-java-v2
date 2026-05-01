package br.com.wdc.shopping.test.repository;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.test.util.ResetDatabaseRule;
import br.com.wdc.shopping.test.util.TestEnvironment;

public class RestUserRepositoryTest extends AbstractUserRepositoryTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST);

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	@Override
	protected UserRepository repo() {
		return env.userRepo();
	}
}
