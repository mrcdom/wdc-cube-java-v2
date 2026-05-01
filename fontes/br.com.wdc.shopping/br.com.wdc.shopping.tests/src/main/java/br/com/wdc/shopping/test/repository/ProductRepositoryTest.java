package br.com.wdc.shopping.test.repository;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.test.util.ResetDatabaseRule;
import br.com.wdc.shopping.test.util.TestEnvironment;

public class ProductRepositoryTest extends AbstractProductRepositoryTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	@Override
	protected ProductRepository repo() {
		return env.productRepo();
	}
}
