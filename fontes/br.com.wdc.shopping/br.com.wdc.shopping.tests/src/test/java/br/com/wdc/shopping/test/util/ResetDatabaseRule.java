package br.com.wdc.shopping.test.util;

import org.junit.rules.ExternalResource;

/**
 * Rule que reseta o banco de dados antes de cada teste.
 */
public class ResetDatabaseRule extends ExternalResource {

	private final TestEnvironment env;

	public ResetDatabaseRule(TestEnvironment env) {
		this.env = env;
	}

	@Override
	protected void before() {
		env.resetDatabase();
	}
}
