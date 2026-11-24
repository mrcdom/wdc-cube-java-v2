package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.security.PrivateKey;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.domain.security.policy.DataPolicy;
import br.com.wdc.framework.domain.security.policy.Mask;
import br.com.wdc.framework.domain.security.policy.RowScope;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.persistence.rest.security.UserDataPolicy;

/**
 * The rules of {@code DataAccess}, on policies written for the test over the user descriptor — no database: what is
 * checked is how a request is refused or rewritten, and how a row is shaped.
 */
public class DataPolicyTest {

	private static final ProjectionValues PV = ProjectionValues.INSTANCE;

	private static final Long ME = 7L;

	/** Rows named "Team" — a scope that is not about the key, to test moving a row out of reach. */
	private static final RowScope<User, UserCriteria> TEAM = new RowScope<>() {

		@Override
		public void restrict(UserCriteria group, SecurityContext sc) {
			group.withName("Team");
		}

		@Override
		public boolean covers(User row, SecurityContext sc) {
			return "Team".equals(row.name());
		}

		@Override
		public Set<String> fields() {
			return Set.of("name");
		}
	};

	// :: Filtering

	@Test
	public void filteringByAFieldNoGrantReads_isRefusedByName() {
		var policy = DataPolicy.on(UserDataPolicy.USER)
				.grant("R", g -> g
						.read(new User()
								.withName(PV.str)))
				.build();

		var e = assertThrows(AccessDeniedException.class,
				() -> policy.accessFor(as("R")).prepareRead(new UserCriteria().userName().startingWith("a")));

		assertTrue(e.getMessage(), e.getMessage().contains("[userName]"));
	}

	@Test
	public void filteringByTwoFieldsThatNoSingleGrantReads_isRefused() {
		// Each field is read by one grant, over different rows: filtering by both at once would reveal the pair
		var policy = DataPolicy.on(UserDataPolicy.USER)
				.grant("R", g -> g
						.read(new User()
								.withName(PV.str)))
				.grant("R", g -> g
						.rows(UserDataPolicy.OWN)
						.read(new User()
								.withUserName(PV.str)))
				.build();

		var criteria = new UserCriteria()
				.withName("x")
				.withUserName("y");

		var e = assertThrows(AccessDeniedException.class, () -> policy.accessFor(as("R")).prepareRead(criteria));

		assertTrue(e.getMessage(), e.getMessage().contains("userName") && e.getMessage().contains("name"));
	}

	@Test
	public void filteringInsideAnAlternativeGroup_isCheckedToo() {
		var policy = DataPolicy.on(UserDataPolicy.USER)
				.grant("R", g -> g
						.read(new User()
								.withName(PV.str)))
				.build();

		var criteria = new UserCriteria()
				.withAnyOf(new UserCriteria().roles().containing("ADMIN"));

		assertThrows(AccessDeniedException.class, () -> policy.accessFor(as("R")).prepareRead(criteria));
	}

	// :: Rows

	@Test
	public void aGrantOverEveryRow_addsNoRestriction() {
		var policy = DataPolicy.on(UserDataPolicy.USER)
				.grant("R", g -> g
						.rows(UserDataPolicy.OWN)
						.read(login()))
				.grant("R", g -> g
						.read(login()))
				.build();

		var criteria = new UserCriteria();
		policy.accessFor(as("R")).prepareRead(criteria);

		assertTrue(criteria.anyOf().isEmpty());
	}

	@Test
	public void scopedGrants_becomeAlternativeGroups_carryingTheClientGroups() {
		var policy = DataPolicy.on(UserDataPolicy.USER)
				.grant("A", g -> g
						.rows(UserDataPolicy.OWN)
						.read(login()))
				.grant("B", g -> g
						.rows(TEAM)
						.read(login()))
				.build();

		var clientGroup = new UserCriteria()
				.withUserName("jdoe");
		var criteria = new UserCriteria()
				.withAnyOf(clientGroup);
		policy.accessFor(as("A", "B")).prepareRead(criteria);

		assertEquals("one group per scope", 2, criteria.anyOf().size());
		for (var group : criteria.anyOf()) {
			assertEquals("the client's alternatives hold inside each scope", List.of(clientGroup), group.anyOf());
		}
		assertTrue(criteria.anyOf().stream().anyMatch(UserCriteria::hasUserId));
		assertTrue(criteria.anyOf().stream().anyMatch(UserCriteria::hasName));
	}

	@Test
	public void theRowCheckFields_areFetched_butNotReturned() {
		var policy = DataPolicy.on(UserDataPolicy.USER)
				.grant("R", g -> g
						.rows(TEAM)
						.read(new User()
								.withUserName(PV.str)))
				.build();

		var criteria = new UserCriteria()
				.withProjection(new User()
						.withUserName(PV.str));
		var plan = policy.accessFor(as("R")).prepareRead(criteria);

		assertEquals("name is fetched for the row check", "~", criteria.projection().name());

		var row = plan.apply(new User()
				.withId(1L)
				.withUserName("jdoe")
				.withName("Team"));
		assertEquals("jdoe", row.userName());
		assertNull("and cleared before the response", row.name());
	}

	@Test
	public void eachRow_showsWhatTheGrantsThatReachItAllow() {
		var policy = DataPolicy.on(UserDataPolicy.USER)
				.grant("R", g -> g
						.rows(UserDataPolicy.OWN)
						.read(login()))
				.grant("R", g -> g
						.read(new User()
								.withName(PV.str))
						.mask(new User()
								.withUserName(PV.str), Mask.keepLast(2)))
				.build();

		var plan = policy.accessFor(as("R")).prepareRead(new UserCriteria()
				.withProjection(login()));

		var own = plan.apply(new User()
				.withId(ME)
				.withUserName("myself")
				.withName("Me"));
		var other = plan.apply(new User()
				.withId(1L)
				.withUserName("someone")
				.withName("Other"));

		assertEquals("myself", own.userName());
		assertEquals("*****ne", other.userName());
		assertEquals("Other", other.name());
	}

	// :: Writing

	@Test
	public void anUpdateThatMovesTheRowOutOfReach_isRefused() {
		var policy = DataPolicy.on(UserDataPolicy.USER)
				.grant("R", g -> g
						.rows(TEAM)
						.read(login())
						.update(login()))
				.build();
		var access = policy.accessFor(as("R"));
		var stored = new User()
				.withId(1L)
				.withName("Team");

		// Within reach: allowed
		access.checkUpdate(new User()
				.withId(1L)
				.withUserName("renamed"), new User()
						.withUserName(PV.str), (row, projection) -> stored);

		// Out of it: refused
		assertThrows(AccessDeniedException.class, () -> access.checkUpdate(new User()
				.withId(1L)
				.withName("Elsewhere"), new User()
						.withName(PV.str), (row, projection) -> stored));
	}

	@Test
	public void aFieldOutsideTheGrant_cannotBeUpdated() {
		var policy = DataPolicy.on(UserDataPolicy.USER)
				.grant("R", g -> g
						.rows(UserDataPolicy.OWN)
						.read(login())
						.update(new User()
								.withName(PV.str)))
				.build();

		var e = assertThrows(AccessDeniedException.class, () -> policy.accessFor(as("R")).checkUpdate(new User()
				.withId(ME)
				.withRoles("ADMIN"), new User()
						.withRoles(PV.str), (row, projection) -> new User()
								.withId(ME)));

		assertTrue(e.getMessage(), e.getMessage().contains("[roles]"));
	}

	// :: Declaration

	@Test
	public void aMaskedFieldCannotBeWrittenByTheSameGrant() {
		assertThrows(IllegalStateException.class, () -> DataPolicy.on(UserDataPolicy.USER)
				.grant("R", g -> g
						.mask(new User()
								.withUserName(PV.str), Mask.keepFirst(1))
						.update(new User()
								.withUserName(PV.str))));
	}

	@Test
	public void theKeyCannotBeMasked() {
		assertThrows(IllegalStateException.class, () -> DataPolicy.on(UserDataPolicy.USER)
				.grant("R", g -> g
						.mask(new User()
								.withId(PV.i64), Mask.keepFirst(1))));
	}

	@Test
	public void withoutSecurity_everythingPasses() {
		var criteria = new UserCriteria()
				.withUserName("jdoe");

		UserDataPolicy.POLICY.accessFor(null).prepareRead(criteria);

		assertNull(criteria.projection());
		assertTrue(criteria.anyOf().isEmpty());
	}

	// :: Helpers

	private static User login() {
		return new User()
				.withUserName(PV.str)
				.withName(PV.str);
	}

	private static SecurityContext as(String... roles) {
		return new SecurityContext() {

			@Override
			public Long userId() {
				return ME;
			}

			@Override
			public String userName() {
				return "me";
			}

			@Override
			public Set<String> roles() {
				return Set.of(roles);
			}

			@Override
			public Set<String> permissions() {
				return Set.of();
			}

			@Override
			public boolean hasPermission(String entity, String operation) {
				return false;
			}

			@Override
			public boolean hasDataAll() {
				return false;
			}

			@Override
			public PrivateKey privateKey() {
				return null;
			}

			@Override
			public String publicKeyBase64() {
				return null;
			}
		};
	}
}
