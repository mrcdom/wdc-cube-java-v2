package br.com.wdc.shopping.scripts.sgbd;

import static br.com.wdc.shopping.persistence.jooq.Sequences.SQ_PRODUCT;
import static br.com.wdc.shopping.persistence.jooq.Sequences.SQ_PURCHASE;
import static br.com.wdc.shopping.persistence.jooq.Sequences.SQ_PURCHASEITEM;
import static br.com.wdc.shopping.persistence.jooq.Sequences.SQ_USER;
import static br.com.wdc.shopping.persistence.jooq.tables.EnProduct.EN_PRODUCT;
import static br.com.wdc.shopping.persistence.jooq.tables.EnPurchase.EN_PURCHASE;
import static br.com.wdc.shopping.persistence.jooq.tables.EnPurchaseitem.EN_PURCHASEITEM;
import static br.com.wdc.shopping.persistence.jooq.tables.EnUser.EN_USER;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import br.com.wdc.framework.jooq.JooqDSLContext;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;

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

	public static void run() {
		var dsl = JooqDSLContext.BEAN.get();

		/*
		 * Clean all
		 */

		dsl.deleteFrom(EN_PURCHASEITEM).execute();
		dsl.deleteFrom(EN_PURCHASE).execute();
		dsl.deleteFrom(EN_PRODUCT).execute();
		dsl.deleteFrom(EN_USER).execute();

		long id;

		/*
		 * Users
		 */

		id = 0;
		addUser(DBReset.ADMIN_ID = id++, "admin", "admin", "João da Silva", "ADMIN");
		addUser(DBReset.FULANO_ID = id++, "fulano", "fulano", "Fulano de Tal", "CUSTOMER");
		addUser(DBReset.BEOTRANO_ID = id++, "beotrano", "beotrano", "Beotrano de Alguma Coisa", "CUSTOMER");
		dsl.alterSequence(SQ_USER).restartWith(id).execute();

		/*
		 * Products
		 */

		id = 0;
		addProduct(DBReset.CAFETEIRA_ID = id++, "Cafeteira design italiano", 199.99, "<ul>" + "<li>Capacidade para 30 cafés (50ml cada) ou 24 cafés (62ml cada)</li>" + "<li>Sistema corta-pingos</li>"
				+ "<li>Acompanha filtro permanente removível e colher medidora</li>" + "<li>Permite uso de filtro de papel</li>"
				+ "<li>Reservatório de água com graduação</li>" + "<li>Botão luminoso liga/desliga</li>" + "<li>Fácil de lavar</li>"
				+ "<li>Peças podem ser lavadas em máquina de lavar louça (exceto a base motora)</li>"
				+ "<li>Potência: 1000W - correspondente a 1 Kwh (Kilowatts hora).</li>" + "</ul>",
				"images/cafeteira.png");

		addProduct(DBReset.BOLA_WILSON_ID = id++, "Bola Wilson", 45.30, "<ul>" + "<li>Bola Wilson Tamanho e Peso Oficial.</li>"
				+ "<li>Garantia: Contra defeito de fabricação.</li>" + "<li>Origem: Importada.</li>" + "</ul>", "images/wilson.png");

		addProduct(DBReset.FITA_VEDA_ROSCA_ID = id++, "Fita veda rosca", 2.67, "<ul>" + "<li>Marca Tigre.</li>" + "<li>Tamanho e medida: 18 mm x 10 m.</li>"
				+ "<li>Composição: Teflon.</li>" + "<li>Utilização: vedação de juntas roscaveis.</li>" + "</ul>", "images/vedarosca.png");

		addProduct(DBReset.PEN_DRIVE2GB_ID = id++, "Pen Drive 2GB", 16.0, "Ideal para transporte de arquivos de dados, áudio, vídeo, "
				+ "fotos e muito mais. Melhor valor para armazenamento e transferência de informação. Portátil, "
				+ "fácil de usar e super leve, ele possui segurança com seus dados, led indicando o uso, além "
				+ "de ser resistente a quedas. Pen Drive com capacidade de armazenamento de 2 GB, praticidade " + "e qualidade com seus arquivos!",
				"images/pendrive2gb.png");

		dsl.alterSequence(SQ_PRODUCT).restartWith(id).execute();

		/*
		 * Purchases
		 */

		id = 0;

		DBReset.ADMIN_FIRST_PURCHASE_ID = id++;
		DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID = 0L;
		addPurchase(DBReset.ADMIN_FIRST_PURCHASE_ID, DBReset.ADMIN_ID, OffsetDateTime.of(2010, 1, 1, 14, 30, 0, 0, ZoneOffset.UTC),
				newItem(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, DBReset.CAFETEIRA_ID, 1, 200.0));

		DBReset.ADMIN_SECOND_PURCHASE_ID = id++;
		DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID = 1L;
		DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID = 2L;
		addPurchase(DBReset.ADMIN_SECOND_PURCHASE_ID, DBReset.ADMIN_ID, OffsetDateTime.of(2011, 4, 3, 9, 15, 0, 0, ZoneOffset.UTC),
				newItem(DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID, DBReset.BOLA_WILSON_ID, 1, 45.30),
				newItem(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID, DBReset.FITA_VEDA_ROSCA_ID, 1, 2.67));

		dsl.alterSequence(SQ_PURCHASE).restartWith(id).execute();
		dsl.alterSequence(SQ_PURCHASEITEM).restartWith(3L).execute();
	}

	private static void addUser(long id, String userName, String password, String name, String roles) {
		var userRepo = UserRepository.BEAN.get();
		
		var user = new User();
		user.id = id;
		user.userName = userName;
		user.name = name;
		user.roles = roles;
		if (StringUtils.isNotBlank(password)) {
			user.password = new BigInteger(md5().digest(password.getBytes(StandardCharsets.UTF_8))).toString(36);
		}
		userRepo.insert(user);
	}

	private static void addProduct(long id, String name, double price, String description, String imageResource) {
		var productRepo = ProductRepository.BEAN.get();
		
		var product = new Product();
		product.id = id;
		product.name = name;
		product.price = price;
		product.description = description;

		if (imageResource != null) {
			InputStream imageStream = DBReset.class.getResourceAsStream("/META-INF/" + imageResource);
			if (imageStream != null) {
				try (imageStream) {
					product.image = IOUtils.toByteArray(imageStream);
				} catch (IOException caught) {
					throw ExceptionUtils.asRuntimeException(caught);
				}
			}
		}

		productRepo.insert(product);
	}

	private static PurchaseItem newItem(long id, long productId, int amount, double price) {
		var item = new PurchaseItem();
		item.id = id;
		item.product = new Product();
		item.product.id = productId;
		item.amount = amount;
		item.price = price;
		return item;
	}

	private static void addPurchase(long id, long userId, OffsetDateTime buyDate, PurchaseItem... items) {
		var purchaseRepo = PurchaseRepository.BEAN.get();
		
		var purchase = new Purchase();
		purchase.id = id;
		purchase.user = new User();
		purchase.user.id = userId;
		purchase.buyDate = buyDate;
		purchase.items = List.of(items);
		purchaseRepo.insert(purchase);
	}

	private static MessageDigest md5() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException caught) {
			throw ExceptionUtils.asRuntimeException(caught);
		}
	}

}
