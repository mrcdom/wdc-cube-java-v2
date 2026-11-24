package br.com.wdc.store.test.repository;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.TestEnvironment;

public class UserRepositoryTest extends AbstractUserRepositoryTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	@Override
	protected UserRepository repo() {
		return env.userRepo();
	}

	// :: Alternative groups (anyOf) — server-side only, so only here; the REST side refuses them

	@Test
	public void anyOf_isADisjunctionAcrossFields() {
		// admin by login, or Jane Roe by name: two fields that or() on a single field cannot join
		var criteria = new UserCriteria()
				.withAnyOf(
						new UserCriteria()
								.withUserName("admin"),
						new UserCriteria().name().containing("Roe"));

		var logins = repo().fetch(criteria).stream().map(User::userName).sorted().toList();

		assertEquals(List.of("admin", "jroe"), logins);
	}

	@Test
	public void anyOf_holdsBesidesTheOuterFields() {
		var criteria = new UserCriteria().roles().eq("CUSTOMER")
				.withAnyOf(
						new UserCriteria()
								.withUserName("jdoe"),
						new UserCriteria()
								.withUserName("admin"));

		var users = repo().fetch(criteria);

		assertEquals("admin is not a CUSTOMER, so only jdoe is left", 1, users.size());
		assertEquals("jdoe", users.get(0).userName());
		assertEquals(1, repo().count(criteria));
	}

	@Test
	public void anyOf_aGroupWithNoFieldMatchesEveryRow() {
		var criteria = new UserCriteria()
				.withAnyOf(
						new UserCriteria(),
						new UserCriteria()
								.withUserName("admin"));

		assertEquals(3, repo().fetch(criteria).size());
	}

	@Test
	public void anyOf_groupsNest() {
		// CUSTOMER and (jdoe or jroe), or admin
		var customers = new UserCriteria().roles().eq("CUSTOMER")
				.withAnyOf(
						new UserCriteria()
								.withUserName("jdoe"),
						new UserCriteria()
								.withUserName("jroe"));
		var criteria = new UserCriteria()
				.withAnyOf(
						customers,
						new UserCriteria()
								.withUserName("admin"));

		assertEquals(3, repo().fetch(criteria).size());
		assertEquals(3, repo().count(criteria));
	}

	@Test
	public void anyOf_noGroupMatches_returnsNothing() {
		var criteria = new UserCriteria()
				.withAnyOf(
						new UserCriteria()
								.withUserName("nobody"),
						new UserCriteria().name().containing("Nobody"));

		assertTrue(repo().fetch(criteria).isEmpty());
	}
}
