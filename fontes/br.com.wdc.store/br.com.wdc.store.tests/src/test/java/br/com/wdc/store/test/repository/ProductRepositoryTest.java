package br.com.wdc.store.test.repository;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.TestEnvironment;

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
