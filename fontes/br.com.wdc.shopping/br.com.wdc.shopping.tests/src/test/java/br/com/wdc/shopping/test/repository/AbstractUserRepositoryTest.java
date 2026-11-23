package br.com.wdc.shopping.test.repository;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.user.User;
import br.com.wdc.shopping.domain.user.UserCriteria;
import br.com.wdc.shopping.domain.user.UserRepository;
import br.com.wdc.shopping.scripts.sgbd.DBReset;

public abstract class AbstractUserRepositoryTest {

	protected abstract UserRepository repo();

	// :: fetch

	@Test
	public void fetchAll_returnsAllSeededUsers() {
		List<User> users = repo().fetch(new UserCriteria());
		assertEquals(3, users.size());
	}

	@Test
	public void fetchById_returnsCorrectUser() {
		var user = repo().fetchById(DBReset.ADMIN_ID, null);
		assertNotNull(user);
		assertEquals("admin", user.userName());
		assertEquals("John Smith", user.name());
	}

	@Test
	public void fetchById_nonExistent_returnsNull() {
		var user = repo().fetchById(Long.MAX_VALUE, null);
		assertNull(user);
	}

	@Test
	public void fetchWithProjection_onlyRequestedFields() {
		var pv = ProjectionValues.INSTANCE;
		var projection = new User()
				.withId(pv.i64)
				.withUserName(pv.str);

		var user = repo().fetchById(DBReset.ADMIN_ID, projection);
		assertNotNull(user);
		assertEquals(DBReset.ADMIN_ID, user.id());
		assertEquals("admin", user.userName());
	}

	@Test
	public void fetchByCriteria_userName() {
		var users = repo().fetch(new UserCriteria()
				.withUserName("jdoe"));
		assertEquals(1, users.size());
		assertEquals(DBReset.JOHN_DOE_ID, users.get(0).id());
	}

	@Test
	public void fetchWithOffsetAndLimit() {
		var users = repo().fetch(new UserCriteria()
				.withOrderBy(UserCriteria.OrderBy.OLDEST_FIRST), 1, 1);
		assertEquals(1, users.size());
	}

	// :: count

	@Test
	public void countAll_returnsThree() {
		int count = repo().count(new UserCriteria());
		assertEquals(3, count);
	}

	@Test
	public void countByUserId_returnsOne() {
		int count = repo().count(new UserCriteria()
				.withUserId(DBReset.ADMIN_ID));
		assertEquals(1, count);
	}

	@Test
	public void countByNonExistentId_returnsZero() {
		int count = repo().count(new UserCriteria()
				.withUserId(Long.MAX_VALUE));
		assertEquals(0, count);
	}

	// :: insert

	@Test
	public void insert_newUser() {
		var user = new User()
				.withUserName("newuser")
				.withPassword("secret")
				.withName("New User");

		boolean inserted = repo().insert(user);
		assertTrue(inserted);
		assertNotNull(user.id());

		var fetched = repo().fetchById(user.id(), null);
		assertNotNull(fetched);
		assertEquals("newuser", fetched.userName());
		assertEquals("New User", fetched.name());
	}

	// :: update

	@Test
	public void update_existingUser() {
		var pv = ProjectionValues.INSTANCE;
		var fullProjection = new User()
				.withId(pv.i64)
				.withUserName(pv.str)
				.withPassword(pv.str)
				.withName(pv.str);

		var original = repo().fetchById(DBReset.ADMIN_ID, fullProjection);
		assertNotNull(original);

		var updated = new User()
				.withId(original.id())
				.withUserName(original.userName())
				.withPassword(original.password())
				.withName("Changed Name");

		boolean result = repo().update(updated, original);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_ID, null);
		assertEquals("Changed Name", fetched.name());
	}

	@Test
	public void update_partialFields_onlyChangesSpecifiedFields() {
		var pv = ProjectionValues.INSTANCE;
		var fullProjection = new User()
				.withId(pv.i64)
				.withUserName(pv.str)
				.withPassword(pv.str)
				.withName(pv.str)
				.withRoles(pv.str);

		var original = repo().fetchById(DBReset.ADMIN_ID, fullProjection);
		assertNotNull(original);
		var originalUserName = original.userName();
		var originalPassword = original.password();

		// partial projection: only id and name
		var projection = new User()
				.withId(pv.i64)
				.withName(pv.str);

		var updated = new User()
				.withId(original.id())
				.withName("Partial Name");

		boolean result = repo().update(updated, original, projection);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_ID, fullProjection);
		assertEquals("Partial Name", fetched.name());
		assertEquals(originalUserName, fetched.userName());
		assertEquals(originalPassword, fetched.password());
	}

	@Test
	public void update_setFieldToNull_clearsValue() {
		var pv = ProjectionValues.INSTANCE;
		var fullProjection = new User()
				.withId(pv.i64)
				.withUserName(pv.str)
				.withPassword(pv.str)
				.withName(pv.str)
				.withRoles(pv.str);

		// Ensures admin has roles filled in
		var original = repo().fetchById(DBReset.ADMIN_ID, fullProjection);
		assertNotNull(original);
		assertNotNull(original.roles());

		// the projection includes roles — the value is null to clear it
		var projection = new User()
				.withId(pv.i64)
				.withRoles(pv.str);

		var updated = new User()
				.withId(original.id());
		updated.withRoles(null); // intentionally cleared

		boolean result = repo().update(updated, original, projection);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_ID, fullProjection);
		assertNull(fetched.roles());
		// other fields untouched
		assertEquals(original.userName(), fetched.userName());
		assertEquals(original.name(), fetched.name());
	}

	// :: delete

	@Test
	public void deleteByUserId_noFkDependency() {
		// JANE_ROE has no purchases, so no FK violation
		int deleted = repo().delete(new UserCriteria()
				.withUserId(DBReset.JANE_ROE_ID));
		assertEquals(1, deleted);
		assertEquals(2, repo().count(new UserCriteria()));
	}

	@Test
	public void deleteNonExistent_returnsZero() {
		int deleted = repo().delete(new UserCriteria()
				.withUserId(Long.MAX_VALUE));
		assertEquals(0, deleted);
	}

	// :: Text criterion — these also run in REST mode, where they prove the operators travel over the wire

	@Test
	public void startingWith_anchorsAtTheBeginning() {
		var users = repo().fetch(new UserCriteria().userName().startingWith("adm"));

		assertEquals(1, users.size());
		assertEquals("admin", users.get(0).userName());
	}

	@Test
	public void startingWith_doesNotMatchInTheMiddle() {
		// What sets startingWith apart from containing: "dmi" occurs in "admin", but not at the start.
		assertEquals(0, repo().fetch(new UserCriteria().userName().startingWith("dmi")).size());
	}

	@Test
	public void containing_matchesAnywhere() {
		var users = repo().fetch(new UserCriteria().userName().containing("doe"));

		assertEquals(1, users.size());
		assertEquals("jdoe", users.get(0).userName());
	}

	@Test
	public void ilike_ignoresCase() {
		var users = repo().fetch(new UserCriteria().userName().ilike("ADMIN"));

		assertEquals(1, users.size());
	}

	@Test
	public void like_doesNotIgnoreCase() {
		// This pair is why there are two operators instead of one.
		assertEquals(0, repo().fetch(new UserCriteria().userName().like("ADMIN")).size());
		assertEquals(1, repo().fetch(new UserCriteria().userName().like("admin")).size());
	}

	@Test
	public void wildcardsBelongToTheValue() {
		// like("admin") is not like("%admin%"): the wildcards are part of the value, as in SQL.
		assertEquals(0, repo().fetch(new UserCriteria().userName().like("dmi")).size());
		assertEquals(1, repo().fetch(new UserCriteria().userName().like("%dmi%")).size());
	}

	@Test
	public void or_acrossTextRequests() {
		var criteria = new UserCriteria();
		criteria.userName().or().eq("admin");
		criteria.userName().eq("jdoe");

		assertEquals(2, repo().fetch(criteria).size());
	}

	@Test
	public void emptyText_doesNotFilter() {
		var criteria = new UserCriteria();
		criteria.userName().startingWith("");

		assertEquals("empty text adds no request", 3, repo().fetch(criteria).size());
	}

	// :: Fields that are not the key

	@Test
	public void filterByName_text() {
		var users = repo().fetch(new UserCriteria().name().containing("Smith"));

		assertEquals(1, users.size());
		assertEquals("admin", users.get(0).userName());
	}

	@Test
	public void filterByRoles() {
		assertEquals(1, repo().fetch(new UserCriteria().roles().eq("ADMIN")).size());
		assertEquals(2, repo().fetch(new UserCriteria().roles().eq("CUSTOMER")).size());
	}

	@Test
	public void filterByRoles_containing() {
		// Roles are comma-separated in the column; containing is the filter that answers "has this role".
		assertEquals(1, repo().fetch(new UserCriteria().roles().containing("ADMIN")).size());
	}

	// :: Provisioned orderings

	@Test
	public void orderBy_nameAToZ() {
		var names = repo().fetch(new UserCriteria()
				.withOrderBy(UserCriteria.OrderBy.NAME_A_TO_Z)).stream().map(User::name).toList();

		assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), names);
	}

	@Test
	public void orderBy_loginAToZ_differsFromName() {
		var byLogin = repo().fetch(new UserCriteria()
				.withOrderBy(UserCriteria.OrderBy.LOGIN_A_TO_Z)).stream().map(User::userName).toList();

		assertEquals(byLogin.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), byLogin);

		// Ordering by login is not the same as ordering by name: they are distinct concepts, and that is why
		// there are two constants instead of a single "alphabetical order".
		var byName = repo().fetch(new UserCriteria()
				.withOrderBy(UserCriteria.OrderBy.NAME_A_TO_Z)).stream().map(User::userName).toList();
		assertNotEquals(byName, byLogin);
	}
}
