package br.com.wdc.store.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import br.com.wdc.store.domain.security.Role;

public class RoleTest {

	@Test
	public void everyRoleReadsItsOwnUser() {
		// The client reads the signed-in user's name right after login (teavm.web goes through REST for it)
		for (var role : Role.values()) {
			assertTrue(role + " reads users", Role.hasPermission(role.permissions(), "user", "read"));
		}
	}

	@Test
	public void onlyAdminSeesEveryonesData() {
		assertTrue(Role.hasDataAll(Role.ADMIN.permissions()));
		assertFalse("CUSTOMER reads only its own records", Role.hasDataAll(Role.CUSTOMER.permissions()));
		assertFalse("MANAGER reads only its own records", Role.hasDataAll(Role.MANAGER.permissions()));
	}

	@Test
	public void onlyAdminChangesUsers() {
		assertTrue(Role.hasPermission(Role.ADMIN.permissions(), "user", "write"));
		assertFalse(Role.hasPermission(Role.CUSTOMER.permissions(), "user", "write"));
		assertFalse(Role.hasPermission(Role.MANAGER.permissions(), "user", "write"));
	}
}
