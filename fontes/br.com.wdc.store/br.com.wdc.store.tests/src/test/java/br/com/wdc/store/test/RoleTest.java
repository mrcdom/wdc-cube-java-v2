package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import br.com.wdc.store.domain.security.Role;

public class RoleTest {

	@Test
	public void parse_readsTheCommaSeparatedRoles() {
		assertEquals(Set.of(Role.ADMIN, Role.CUSTOMER), Role.parse(" ADMIN , CUSTOMER "));
	}

	@Test
	public void parse_ignoresUnknownRoles() {
		// A role removed from the code does not lock its users out, nor grant anything
		assertEquals(Set.of(Role.MANAGER), Role.parse("MANAGER,SUPPORT"));
		assertTrue(Role.parse(null).isEmpty());
		assertTrue(Role.parse(" ").isEmpty());
	}

	@Test
	public void names_areWhatThePoliciesGrantBy() {
		assertEquals(Set.of("ADMIN", "MANAGER"), Role.names(Set.of(Role.ADMIN, Role.MANAGER)));
	}
}
