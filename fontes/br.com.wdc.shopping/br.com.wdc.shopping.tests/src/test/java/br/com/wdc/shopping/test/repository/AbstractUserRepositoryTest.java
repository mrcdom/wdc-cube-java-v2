package br.com.wdc.shopping.test.repository;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
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
		assertEquals("admin", user.userName);
		assertEquals("João da Silva", user.name);
	}

	@Test
	public void fetchById_nonExistent_returnsNull() {
		var user = repo().fetchById(Long.MAX_VALUE, null);
		assertNull(user);
	}

	@Test
	public void fetchWithProjection_onlyRequestedFields() {
		var pv = ProjectionValues.INSTANCE;
		var projection = new User();
		projection.id = pv.i64;
		projection.userName = pv.str;

		var user = repo().fetchById(DBReset.ADMIN_ID, projection);
		assertNotNull(user);
		assertEquals(DBReset.ADMIN_ID, user.id);
		assertEquals("admin", user.userName);
	}

	@Test
	public void fetchByCriteria_userName() {
		var users = repo().fetch(new UserCriteria().withUserName("fulano"));
		assertEquals(1, users.size());
		assertEquals(DBReset.FULANO_ID, users.get(0).id);
	}

	@Test
	public void fetchByCriteria_userNameAndPassword() {
		var users = repo().fetch(new UserCriteria()
				.withUserName("admin")
				.withPassword("admin"));
		assertEquals(1, users.size());
		assertEquals(DBReset.ADMIN_ID, users.get(0).id);
	}

	@Test
	public void fetchByCriteria_wrongPassword_returnsEmpty() {
		var users = repo().fetch(new UserCriteria()
				.withUserName("admin")
				.withPassword("wrong"));
		assertTrue(users.isEmpty());
	}

	@Test
	public void fetchWithOffsetAndLimit() {
		var users = repo().fetch(new UserCriteria()
				.withOrderBy(UserCriteria.OrderBy.ACENDING)
				.withOffset(1)
				.withLimit(1));
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
		int count = repo().count(new UserCriteria().withUserId(DBReset.ADMIN_ID));
		assertEquals(1, count);
	}

	@Test
	public void countByNonExistentId_returnsZero() {
		int count = repo().count(new UserCriteria().withUserId(Long.MAX_VALUE));
		assertEquals(0, count);
	}

	// :: insert

	@Test
	public void insert_newUser() {
		var user = new User();
		user.userName = "newuser";
		user.password = "secret";
		user.name = "New User";

		boolean inserted = repo().insert(user);
		assertTrue(inserted);
		assertNotNull(user.id);

		var fetched = repo().fetchById(user.id, null);
		assertNotNull(fetched);
		assertEquals("newuser", fetched.userName);
		assertEquals("New User", fetched.name);
	}

	// :: update

	@Test
	public void update_existingUser() {
		var pv = ProjectionValues.INSTANCE;
		var fullProjection = new User();
		fullProjection.id = pv.i64;
		fullProjection.userName = pv.str;
		fullProjection.password = pv.str;
		fullProjection.name = pv.str;

		var original = repo().fetchById(DBReset.ADMIN_ID, fullProjection);
		assertNotNull(original);

		var updated = new User();
		updated.id = original.id;
		updated.userName = original.userName;
		updated.password = original.password;
		updated.name = "Nome Alterado";

		boolean result = repo().update(updated, original);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_ID, null);
		assertEquals("Nome Alterado", fetched.name);
	}

	// :: insertOrUpdate

	@Test
	public void insertOrUpdate_insertsWhenNew() {
		var user = new User();
		user.userName = "iou_user";
		user.password = "pass";
		user.name = "IOU Test";

		boolean result = repo().insertOrUpdate(user);
		assertTrue(result);
		assertNotNull(user.id);

		var fetched = repo().fetchById(user.id, null);
		assertEquals("IOU Test", fetched.name);
	}

	@Test
	public void insertOrUpdate_updatesWhenExisting() {
		var user = new User();
		user.id = DBReset.ADMIN_ID;
		user.userName = "admin";
		user.password = "admin";
		user.name = "Updated Admin";

		boolean result = repo().insertOrUpdate(user);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_ID, null);
		assertEquals("Updated Admin", fetched.name);
	}

	// :: delete

	@Test
	public void deleteByUserId_noFkDependency() {
		// BEOTRANO has no purchases, so no FK violation
		int deleted = repo().delete(new UserCriteria().withUserId(DBReset.BEOTRANO_ID));
		assertEquals(1, deleted);
		assertEquals(2, repo().count(new UserCriteria()));
	}

	@Test
	public void deleteNonExistent_returnsZero() {
		int deleted = repo().delete(new UserCriteria().withUserId(Long.MAX_VALUE));
		assertEquals(0, deleted);
	}
}
