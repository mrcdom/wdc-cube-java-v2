package br.com.wdc.shopping.scripts.sgbd;

import java.sql.Connection;
import java.sql.SQLException;

import org.jdbi.v3.core.Jdbi;

/**
 * Migration: altera a coluna BUYDATE de DATE para TIMESTAMP na tabela EN_PURCHASE,
 * preservando os dados existentes.
 */
public class Migration_0002_PurchaseBuyDateToTimestamp {

	private final Connection connection;

	public Migration_0002_PurchaseBuyDateToTimestamp(Connection connection) {
		this.connection = connection;
	}

	public void step01_alterBuyDateToTimestamp() throws SQLException {
		try (var handle = Jdbi.create(connection).open()) {
			handle.execute("ALTER TABLE EN_PURCHASE ALTER COLUMN BUYDATE TIMESTAMP NOT NULL");
		}
	}
}
