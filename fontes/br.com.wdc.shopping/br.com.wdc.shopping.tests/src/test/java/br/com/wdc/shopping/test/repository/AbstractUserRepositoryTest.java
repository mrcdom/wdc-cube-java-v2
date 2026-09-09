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
		assertEquals("João da Silva", user.name());
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
		var users = repo().fetch(new UserCriteria().withUserName("fulano"));
		assertEquals(1, users.size());
		assertEquals(DBReset.FULANO_ID, users.get(0).id());
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
				.withName("Nome Alterado");

		boolean result = repo().update(updated, original);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_ID, null);
		assertEquals("Nome Alterado", fetched.name());
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

		// projeção parcial: só id e name
		var projection = new User()
				.withId(pv.i64)
				.withName(pv.str);

		var updated = new User()
				.withId(original.id())
				.withName("Nome Parcial");

		boolean result = repo().update(updated, original, projection);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_ID, fullProjection);
		assertEquals("Nome Parcial", fetched.name());
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

		// Garante que admin tem roles preenchido
		var original = repo().fetchById(DBReset.ADMIN_ID, fullProjection);
		assertNotNull(original);
		assertNotNull(original.roles());

		// projeção inclui roles — valor será null para limpar
		var projection = new User()
				.withId(pv.i64)
				.withRoles(pv.str);

		var updated = new User()
				.withId(original.id());
		updated.withRoles(null); // intencionalmente limpar

		boolean result = repo().update(updated, original, projection);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_ID, fullProjection);
		assertNull(fetched.roles());
		// demais campos intactos
		assertEquals(original.userName(), fetched.userName());
		assertEquals(original.name(), fetched.name());
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

	// :: Critério textual — rodam também no modo REST, onde provam que os operadores trafegam

	@Test
	public void startingWith_anchorsAtTheBeginning() {
		var users = repo().fetch(new UserCriteria().userName().startingWith("adm"));

		assertEquals(1, users.size());
		assertEquals("admin", users.get(0).userName());
	}

	@Test
	public void startingWith_doesNotMatchInTheMiddle() {
		// O que distingue startingWith de containing: "dmi" existe em "admin", mas não no início.
		assertEquals(0, repo().fetch(new UserCriteria().userName().startingWith("dmi")).size());
	}

	@Test
	public void containing_matchesAnywhere() {
		var users = repo().fetch(new UserCriteria().userName().containing("ulan"));

		assertEquals(1, users.size());
		assertEquals("fulano", users.get(0).userName());
	}

	@Test
	public void ilike_ignoresCase() {
		var users = repo().fetch(new UserCriteria().userName().ilike("ADMIN"));

		assertEquals(1, users.size());
	}

	@Test
	public void like_doesNotIgnoreCase() {
		// É o par que justifica existirem os dois operadores em vez de um só.
		assertEquals(0, repo().fetch(new UserCriteria().userName().like("ADMIN")).size());
		assertEquals(1, repo().fetch(new UserCriteria().userName().like("admin")).size());
	}

	@Test
	public void wildcardsBelongToTheValue() {
		// like("admin") não é like("%admin%"): os curingas fazem parte do valor, como em SQL.
		assertEquals(0, repo().fetch(new UserCriteria().userName().like("dmi")).size());
		assertEquals(1, repo().fetch(new UserCriteria().userName().like("%dmi%")).size());
	}

	@Test
	public void or_acrossTextRequests() {
		var criteria = new UserCriteria();
		criteria.userName().or().eq("admin");
		criteria.userName().eq("fulano");

		assertEquals(2, repo().fetch(criteria).size());
	}

	@Test
	public void emptyText_doesNotFilter() {
		var criteria = new UserCriteria();
		criteria.userName().startingWith("");

		assertEquals("texto vazio não acrescenta pedido", 3, repo().fetch(criteria).size());
	}

	// :: Campos que não são a chave

	@Test
	public void filterByName_text() {
		var users = repo().fetch(new UserCriteria().name().containing("Silva"));

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
		// Papéis ficam separados por vírgula na coluna; conter é o filtro que responde "tem este papel".
		assertEquals(1, repo().fetch(new UserCriteria().roles().containing("ADMIN")).size());
	}

	// :: Ordenações provisionadas

	@Test
	public void orderBy_nameAToZ() {
		var nomes = repo().fetch(new UserCriteria()
				.withOrderBy(UserCriteria.OrderBy.NAME_A_TO_Z)).stream().map(User::name).toList();

		assertEquals(nomes.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), nomes);
	}

	@Test
	public void orderBy_loginAToZ_differsFromName() {
		var porLogin = repo().fetch(new UserCriteria()
				.withOrderBy(UserCriteria.OrderBy.LOGIN_A_TO_Z)).stream().map(User::userName).toList();

		assertEquals(porLogin.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), porLogin);

		// Ordenar por login não é o mesmo que ordenar por nome: são conceitos distintos, e é essa a razão
		// de existirem duas constantes em vez de uma "ordem alfabética".
		var porNome = repo().fetch(new UserCriteria()
				.withOrderBy(UserCriteria.OrderBy.NAME_A_TO_Z)).stream().map(User::userName).toList();
		assertNotEquals(porNome, porLogin);
	}
}
