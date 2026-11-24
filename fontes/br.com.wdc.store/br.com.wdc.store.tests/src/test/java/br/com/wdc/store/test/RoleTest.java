package br.com.wdc.store.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import br.com.wdc.store.domain.security.Role;

public class RoleTest {

	@Test
	public void onlyAdminSeesEveryonesData() {
		assertTrue(Role.hasDataAll(Role.ADMIN.permissions()));
		assertFalse("CUSTOMER reads only its own records", Role.hasDataAll(Role.CUSTOMER.permissions()));
		assertFalse("MANAGER reads only its own records", Role.hasDataAll(Role.MANAGER.permissions()));
	}
}
