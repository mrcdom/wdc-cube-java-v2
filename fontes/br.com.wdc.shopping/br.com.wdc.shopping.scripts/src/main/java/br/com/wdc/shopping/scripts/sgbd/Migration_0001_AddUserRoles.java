package br.com.wdc.shopping.scripts.sgbd;

import java.sql.Connection;
import java.sql.SQLException;

import org.jdbi.v3.core.Jdbi;

/**
 * Migration: adiciona coluna ROLES à tabela EN_USER.
 * <p>
 * Todos os usuários existentes recebem o papel CUSTOMER por padrão,
 * exceto 'admin' que recebe ADMIN.
 */
public class Migration_0001_AddUserRoles {

	private final Connection connection;

	public Migration_0001_AddUserRoles(Connection connection) {
		this.connection = connection;
	}

	public void step01_addRolesColumn() throws SQLException {
		try (var handle = Jdbi.create(connection).open()) {
			handle.execute("ALTER TABLE EN_USER ADD COLUMN IF NOT EXISTS ROLES VARCHAR(255) DEFAULT 'CUSTOMER'");
		}
	}

	public void step02_setAdminRole() throws SQLException {
		try (var handle = Jdbi.create(connection).open()) {
			handle.execute("UPDATE EN_USER SET ROLES = 'ADMIN' WHERE USERNAME = 'admin'");
		}
	}
}
