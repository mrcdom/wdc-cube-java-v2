package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.security.PasswordUtil;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.domain.user.UserCodec;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.persistence.client.HttpUserRepository;
import br.com.wdc.store.persistence.client.OkHttpTransport;
import br.com.wdc.store.persistence.client.RestAuthClient;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.TestEnvironment;

/**
 * The user API with authentication on: what each role reads and writes, through {@code UserDataPolicy}.
 *
 * <p>
 * The seed has an ADMIN ({@code admin}) and two CUSTOMERs ({@code jdoe}, {@code jroe}); each test adds a MANAGER
 * ({@code mmajor}), the role that reads other users partially.
 * </p>
 */
public class UserDataPolicyRestTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST).withSecurity();

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	private static final ProjectionValues PV = ProjectionValues.INSTANCE;

	private Long managerId;

	@Before
	public void addManager() {
		var manager = new User()
				.withUserName("mmajor")
				.withPassword(PasswordUtil.hashPassword("mmajor"))
				.withName("Mary Major")
				.withRoles("MANAGER");
		UserRepository.BEAN.get().insert(manager);
		managerId = manager.id();
	}

	// :: Reading

	@Test
	public void customer_readsOnlyTheirOwnUser() {
		var jdoe = signIn("jdoe");

		var users = jdoe.repo().fetch(new UserCriteria()
				.withProjection(login()));

		assertEquals(1, users.size());
		assertEquals("jdoe", users.get(0).userName());
		assertEquals("the count sees the same rows", 1, jdoe.repo().count(new UserCriteria()));
		assertNull("another user is out of reach", jdoe.repo().fetchById(DBReset.ADMIN_ID, login()));
	}

	@Test
	public void customer_fetchByIdOfAnotherUser_findsNothing() {
		var jdoe = signIn("jdoe");

		assertNull(jdoe.transport().postJsonNullable("/api/repo/user/fetch-by-id",
				"{\"id\":" + DBReset.ADMIN_ID + "}"));

		var own = jdoe.transport().postJsonNullable("/api/repo/user/fetch-by-id",
				"{\"id\":" + DBReset.JOHN_DOE_ID + "}");
		assertNotNull(own);
		assertTrue(own, own.contains("\"userName\":\"jdoe\""));
		assertFalse("the password never leaves the server: " + own, own.contains("password"));
	}

	@Test
	public void aFieldNoRoleReads_isRefusedByName() {
		var admin = signIn("admin");

		var e = assertThrows(AccessDeniedException.class, () -> admin.repo().fetch(new UserCriteria()
				.withProjection(new User()
						.withName(PV.str)
						.withPassword(PV.str))));

		assertTrue(e.getMessage(), e.getMessage().contains("[password]"));
	}

	@Test
	public void withoutProjection_returnsWhatTheRoleReads() {
		var admin = signIn("admin");

		var users = admin.repo().fetch(new UserCriteria());

		assertEquals(4, users.size());
		for (var user : users) {
			assertNotNull(user.userName());
			assertNotNull(user.roles());
			assertNull(user.password());
		}
	}

	@Test
	public void manager_seesTheirOwnRowInFull_andTheOthersMasked() {
		var manager = signIn("mmajor");

		var byLogin = byId(manager.repo().fetch(new UserCriteria()
				.withProjection(new User()
						.withUserName(PV.str)
						.withName(PV.str)
						.withRoles(PV.str))));

		assertEquals("every user is reachable", 4, byLogin.size());

		var own = byLogin.get(managerId);
		assertEquals("mmajor", own.userName());
		assertEquals("MANAGER", own.roles());

		var other = byLogin.get(DBReset.JOHN_DOE_ID);
		assertEquals("John Doe", other.name());
		assertEquals("the login of another user comes masked", "j***", other.userName());
		assertNull("and the roles, which only the own grant reads, do not come", other.roles());
	}

	@Test
	public void manager_filteringByAMaskedField_reachesOnlyTheRowsWhereItIsReadable() {
		var manager = signIn("mmajor");

		// Filtering by the login of others would reveal it, letter by letter; only the own grant reads it as it is
		assertTrue(manager.repo().fetch(new UserCriteria().userName().startingWith("j")).isEmpty());
		assertEquals(1, manager.repo().fetch(new UserCriteria()
				.withUserName("mmajor")).size());

		// The same for the ordering by login; ordering by name, which every grant reads, reaches everyone
		assertEquals(1, manager.repo().fetch(new UserCriteria()
				.withOrderBy(UserCriteria.OrderBy.LOGIN_A_TO_Z)).size());
		assertEquals(4, manager.repo().fetch(new UserCriteria()
				.withOrderBy(UserCriteria.OrderBy.NAME_A_TO_Z)).size());
	}

	// :: Writing

	@Test
	public void admin_createsUpdatesAndDeletes() {
		var admin = signIn("admin");

		var created = new User()
				.withUserName("newbie")
				.withPassword(PasswordUtil.hashPassword("newbie"))
				.withName("New Customer")
				.withRoles("CUSTOMER");
		assertTrue(admin.repo().insert(created));

		var renamed = new User()
				.withId(created.id())
				.withName("Renamed Customer");
		assertTrue(admin.repo().update(renamed, new User()
				.withId(created.id())
				.withName("New Customer")));

		assertEquals(1, admin.repo().delete(new UserCriteria()
				.withUserId(created.id())));
	}

	@Test
	public void customer_cannotWriteUsers() {
		var jdoe = signIn("jdoe");

		assertThrows(AccessDeniedException.class, () -> jdoe.repo().insert(new User()
				.withUserName("intruder")
				.withName("Intruder")
				.withRoles("ADMIN")));

		// Not even their own roles
		var e = assertThrows(AccessDeniedException.class, () -> jdoe.repo().update(new User()
				.withId(DBReset.JOHN_DOE_ID)
				.withRoles("ADMIN"), new User()
						.withId(DBReset.JOHN_DOE_ID)
						.withRoles("CUSTOMER")));
		assertTrue(e.getMessage(), e.getMessage().contains("update"));

		assertThrows(AccessDeniedException.class, () -> jdoe.repo().delete(new UserCriteria()
				.withUserId(DBReset.JOHN_DOE_ID)));
	}

	// :: Helpers

	private record Session(OkHttpTransport transport, UserRepository repo) {
	}

	/** Logs in through the REST API — the seed's passwords are the logins. */
	private static Session signIn(String userName) {
		var transport = new OkHttpTransport(env.baseUrl());
		var auth = new RestAuthClient(transport);
		auth.login(userName, PasswordUtil.hashPassword(userName));
		transport.setAccessTokenSupplier(auth::accessToken);
		return new Session(transport, new HttpUserRepository(transport, new UserCodec()));
	}

	private static User login() {
		return new User()
				.withUserName(PV.str)
				.withName(PV.str);
	}

	private static Map<Long, User> byId(List<User> users) {
		return users.stream().collect(Collectors.toMap(User::id, Function.identity()));
	}
}
