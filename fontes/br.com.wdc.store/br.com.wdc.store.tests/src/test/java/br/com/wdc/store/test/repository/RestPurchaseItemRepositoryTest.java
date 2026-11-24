package br.com.wdc.store.test.repository;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

import br.com.wdc.store.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.TestEnvironment;

public class RestPurchaseItemRepositoryTest extends AbstractPurchaseItemRepositoryTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST);

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	@Override
	protected PurchaseItemRepository repo() {
		return env.purchaseItemRepo();
	}
}
