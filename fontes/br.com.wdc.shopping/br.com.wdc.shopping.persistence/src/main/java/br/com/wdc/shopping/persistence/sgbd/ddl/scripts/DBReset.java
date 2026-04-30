package br.com.wdc.shopping.persistence.sgbd.ddl.scripts;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnProduct;
import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnPurchase;
import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnPurchaseItem;
import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnUser;
import br.com.wdc.shopping.persistence.sgbd.repository.product.InsertProductRowCmd;
import br.com.wdc.shopping.persistence.sgbd.repository.purchase.InsertRowPurchaseCmd;
import br.com.wdc.shopping.persistence.sgbd.repository.purchaseitem.InsertRowPurchaseItemCmd;
import br.com.wdc.shopping.persistence.sgbd.repository.user.InsertRowUserCmd;

@SuppressWarnings("java:S3008")
public class DBReset {

	public static Long ADMIN_ID;
	public static Long FULANO_ID;
	public static Long BEOTRANO_ID;

	public static Long CAFETEIRA_ID;
	public static Long BOLA_WILSON_ID;
	public static Long FITA_VEDA_ROSCA_ID;
	public static Long PEN_DRIVE2GB_ID;

	public static Long ADMIN_FIRST_PURCHASE_ID;
	public static Long ADMIN_FIRST_PURCHASE_ITEM0_ID;

	public static Long ADMIN_SECOND_PURCHASE_ID;
	public static Long ADMIN_SECOND_PURCHASE_ITEM0_ID;
	public static Long ADMIN_SECOND_PURCHASE_ITEM1_ID;

	private DBReset() {
		super();
	}

	public static void run(final Connection c) throws SQLException {
		final RunBehaviour bhv = new RunBehaviour(c);

		/*
		 * Clean all
		 */

		for (final String tbName : new String[] {EnPurchaseItem.INSTANCE.tableName(), EnPurchase.INSTANCE.tableName(), EnProduct.INSTANCE.tableName(),
				EnUser.INSTANCE.tableName()}) {
			try (var statement = c.createStatement()) {
				statement.execute("DELETE FROM " + tbName);
			}
		}

		long id;

		/*
		 * Users
		 */

		id = 0;
		bhv.addUser(DBReset.ADMIN_ID = id++, "admin", "admin", "João da Silva");
		bhv.addUser(DBReset.FULANO_ID = id++, "fulano", "fulano", "Fulano de Tal");
		bhv.addUser(DBReset.BEOTRANO_ID = id++, "beotrano", "beotrano", "Beotrano de Alguma Coisa");
		EnUser.INSTANCE.alterSeqUser(c, id);

		/*
		 * Products
		 */

		id = 0;
		bhv.addProduct(DBReset.CAFETEIRA_ID = id++, "Cafeteira design italiano", 199.99,
				"<ul>" + "<li>Capacidade para 30 cafés (50ml cada) ou 24 cafés (62ml cada)</li>" + "<li>Sistema corta-pingos</li>"
						+ "<li>Acompanha filtro permanente removível e colher medidora</li>" + "<li>Permite uso de filtro de papel</li>"
						+ "<li>Reservatório de água com graduação</li>" + "<li>Botão luminoso liga/desliga</li>" + "<li>Fácil de lavar</li>"
						+ "<li>Peças podem ser lavadas em máquina de lavar louça (exceto a base motora)</li>"
						+ "<li>Potência: 1000W - correspondente a 1 Kwh (Kilowatts hora).</li>" + "</ul>",
				"images/cafeteira.png");

		bhv.addProduct(DBReset.BOLA_WILSON_ID = id++, "Bola Wilson", 45.30, "<ul>" + "<li>Bola Wilson Tamanho e Peso Oficial.</li>"
				+ "<li>Garantia: Contra defeito de fabricação.</li>" + "<li>Origem: Importada.</li>" + "</ul>", "images/wilson.png");

		bhv.addProduct(DBReset.FITA_VEDA_ROSCA_ID = id++, "Fita veda rosca", 2.67, "<ul>" + "<li>Marca Tigre.</li>" + "<li>Tamanho e medida: 18 mm x 10 m.</li>"
				+ "<li>Composição: Teflon.</li>" + "<li>Utilização: vedação de juntas roscaveis.</li>" + "</ul>", "images/vedarosca.png");

		bhv.addProduct(DBReset.PEN_DRIVE2GB_ID = id++, "Pen Drive 2GB", 16.0,
				"Ideal para transporte de arquivos de dados, áudio, vídeo, "
						+ "fotos e muito mais. Melhor valor para armazenamento e transferência de informação. Portátil, "
						+ "fácil de usar e super leve, ele possui segurança com seus dados, led indicando o uso, além "
						+ "de ser resistente a quedas. Pen Drive com capacidade de armazenamento de 2 GB, praticidade " + "e qualidade com seus arquivos!",
				"images/pendrive2gb.png");

		EnProduct.INSTANCE.alterSeqProduct(c, id);

		/*
		 * Purchases
		 */

		id = 0;
		bhv.addPurchase(DBReset.ADMIN_FIRST_PURCHASE_ID = id++, DBReset.ADMIN_ID, new int[] {2010, 1, 1});
		bhv.addPurchase(DBReset.ADMIN_SECOND_PURCHASE_ID = id++, DBReset.ADMIN_ID, new int[] {2011, 4, 3});

		EnPurchase.INSTANCE.alterSeqPurchase(c, id);

		/*
		 * Purchases itens
		 */

		id = 0;
		bhv.addPurchaseItem(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID = id++, DBReset.ADMIN_FIRST_PURCHASE_ID, DBReset.CAFETEIRA_ID, 1, 200.0);
		bhv.addPurchaseItem(DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID = id++, DBReset.ADMIN_SECOND_PURCHASE_ID, DBReset.BOLA_WILSON_ID, 1, 45.30);
		bhv.addPurchaseItem(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID = id++, DBReset.ADMIN_SECOND_PURCHASE_ID, DBReset.FITA_VEDA_ROSCA_ID, 1, 2.67);

		EnPurchaseItem.INSTANCE.alterSeqPurchaseItem(c, id);
	}

	private static class RunBehaviour {

		private final Connection c;

		RunBehaviour(Connection c) {
			this.c = c;
		}

		void addUser(long id, String userName, String password, String name) throws SQLException {
			var row = new EnUser.Row();
			row.id(id);
			row.userName(userName);
			if (StringUtils.isNotBlank(password)) {
				var pwd = new BigInteger(md5().digest(password.getBytes(StandardCharsets.UTF_8))).toString(36);
				row.password(pwd);
			}
			row.name(name);

			new InsertRowUserCmd().execute(c, row);
		}

		void addProduct(long id, String name, double price, String description, String image) throws SQLException {
			var row = new EnProduct.Row();
			row.id(id);
			row.name(name);
			row.description(description);
			row.price(BigDecimal.valueOf(price));

			InputStream imageStream = image != null ? DBReset.class.getResourceAsStream("/META-INF/" + image) : null;
			if (imageStream != null) {
				try (imageStream) {
					row.image(IOUtils.toByteArray(imageStream));
				} catch (IOException caught) {
					throw ExceptionUtils.asRuntimeException(caught);
				}
			}
			new InsertProductRowCmd().execute(c, row);
		}

		public void addPurchase(long id, long userId, int[] date) throws SQLException {
			var row = new EnPurchase.Row();
			row.id(id);
			row.userId(userId);
			if (date != null && date.length >= 3) {
				final Calendar cal = Calendar.getInstance();
				cal.set(Calendar.YEAR, date[0]);
				cal.set(Calendar.MONTH, date[1] - 1);
				cal.set(Calendar.DAY_OF_MONTH, date[2]);
				row.buyDate(CoerceUtils.asOffsetDateTime(cal.getTime()));
			}
			new InsertRowPurchaseCmd().execute(c, row);
		}

		public void addPurchaseItem(long id, long purchaseId, long productId, int amount, double price) throws SQLException {
			var row = new EnPurchaseItem.Row();
			row.id(id);
			row.purchaseId(purchaseId);
			row.productId(productId);
			row.amount(amount);
			row.price(BigDecimal.valueOf(price));
			new InsertRowPurchaseItemCmd().execute(c, row);
		}

		private MessageDigest md5() {
			try {
				return MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException caught) {
				throw ExceptionUtils.asRuntimeException(caught);
			}
		}

	}

}
