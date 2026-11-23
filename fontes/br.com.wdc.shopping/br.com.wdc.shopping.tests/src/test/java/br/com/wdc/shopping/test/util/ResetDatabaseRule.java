package br.com.wdc.shopping.test.util;

import org.junit.rules.ExternalResource;

/**
 * Rule that resets the database before each test.
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
