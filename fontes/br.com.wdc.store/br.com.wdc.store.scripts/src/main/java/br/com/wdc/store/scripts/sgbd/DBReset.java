package br.com.wdc.store.scripts.sgbd;

import static br.com.wdc.store.persistence.impl.scheme.Sequences.SQ_PRODUCT;
import static br.com.wdc.store.persistence.impl.scheme.Sequences.SQ_PURCHASE;
import static br.com.wdc.store.persistence.impl.scheme.Sequences.SQ_PURCHASEITEM;
import static br.com.wdc.store.persistence.impl.scheme.Sequences.SQ_USER;
import static br.com.wdc.store.persistence.impl.scheme.tables.EnProduct.EN_PRODUCT;
import static br.com.wdc.store.persistence.impl.scheme.tables.EnPurchase.EN_PURCHASE;
import static br.com.wdc.store.persistence.impl.scheme.tables.EnPurchaseitem.EN_PURCHASEITEM;
import static br.com.wdc.store.persistence.impl.scheme.tables.EnUser.EN_USER;

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

import br.com.wdc.store.domain.product.Product;
import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.domain.purchase.Purchase;
import br.com.wdc.store.domain.purchase.PurchaseRepository;
import br.com.wdc.store.domain.purchaseitem.PurchaseItem;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.persistence.impl.StoreDSLContext;
import br.com.wdc.store.persistence.impl.repository.ProductRepositoryImpl;

@SuppressWarnings("java:S3008")
public class DBReset {

	public static Long ADMIN_ID;
	public static Long JOHN_DOE_ID;
	public static Long JANE_ROE_ID;

	public static Long COFFEE_MAKER_ID;
	public static Long WILSON_BALL_ID;
	public static Long THREAD_SEAL_TAPE_ID;
	public static Long FLASH_DRIVE_2GB_ID;

	public static Long ADMIN_FIRST_PURCHASE_ID;
	public static Long ADMIN_FIRST_PURCHASE_ITEM0_ID;

	public static Long ADMIN_SECOND_PURCHASE_ID;
	public static Long ADMIN_SECOND_PURCHASE_ITEM0_ID;
	public static Long ADMIN_SECOND_PURCHASE_ITEM1_ID;

	private DBReset() {
		super();
	}

	public static void run() {
		var dsl = StoreDSLContext.BEAN.get();

		/*
		 * Clean all
		 */

		dsl.deleteFrom(EN_PURCHASEITEM).execute();
		dsl.deleteFrom(EN_PURCHASE).execute();
		dsl.deleteFrom(EN_PRODUCT).execute();
		dsl.deleteFrom(EN_USER).execute();
		ProductRepositoryImpl.clearImageCache();

		long id;

		/*
		 * Users
		 */

		id = 0;
		addUser(DBReset.ADMIN_ID = id++, "admin", "admin", "John Smith", "ADMIN");
		addUser(DBReset.JOHN_DOE_ID = id++, "jdoe", "jdoe", "John Doe", "CUSTOMER");
		addUser(DBReset.JANE_ROE_ID = id++, "jroe", "jroe", "Jane Roe", "CUSTOMER");
		dsl.alterSequence(SQ_USER).restartWith(id).execute();

		/*
		 * Products
		 */

		id = 0;
		addProduct(DBReset.COFFEE_MAKER_ID = id++, "Italian Design Coffee Maker", 199.99, "<ul>" + "<li>Makes up to 30 cups (50 ml each) or 24 cups (62 ml each)</li>" + "<li>Drip-stop system</li>"
				+ "<li>Includes a removable permanent filter and a measuring spoon</li>" + "<li>Works with paper filters</li>"
				+ "<li>Water reservoir with level markings</li>" + "<li>Illuminated on/off switch</li>" + "<li>Easy to clean</li>"
				+ "<li>Parts are dishwasher-safe (except the motor base)</li>"
				+ "<li>Power: 1000 W - equivalent to 1 kWh (kilowatt-hour).</li>" + "</ul>",
				"images/coffee-maker.png");

		addProduct(DBReset.WILSON_BALL_ID = id++, "Wilson Ball", 45.30, "<ul>" + "<li>Wilson ball, official size and weight.</li>"
				+ "<li>Warranty: covers manufacturing defects.</li>" + "<li>Origin: imported.</li>" + "</ul>", "images/wilson-ball.png");

		addProduct(DBReset.THREAD_SEAL_TAPE_ID = id++, "Thread Seal Tape", 2.67, "<ul>" + "<li>Brand: Tigre.</li>" + "<li>Size: 18 mm x 10 m.</li>"
				+ "<li>Material: Teflon.</li>" + "<li>Use: sealing threaded joints.</li>" + "</ul>", "images/thread-seal-tape.png");

		addProduct(DBReset.FLASH_DRIVE_2GB_ID = id++, "2GB Flash Drive", 16.0, "Ideal for carrying data files, audio, video, "
				+ "photos and much more. The best value for storing and transferring information. Portable, "
				+ "easy to use and super light, it keeps your data safe, has an activity LED and "
				+ "is drop-resistant. A flash drive with 2 GB of storage: convenience " + "and quality for your files!",
				"images/flash-drive-2gb.png");

		dsl.alterSequence(SQ_PRODUCT).restartWith(id).execute();

		/*
		 * Purchases
		 */

		id = 0;

		DBReset.ADMIN_FIRST_PURCHASE_ID = id++;
		DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID = 0L;
		addPurchase(DBReset.ADMIN_FIRST_PURCHASE_ID, DBReset.ADMIN_ID, OffsetDateTime.of(2010, 1, 1, 14, 30, 0, 0, ZoneOffset.UTC),
				newItem(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, DBReset.COFFEE_MAKER_ID, 1, 200.0));

		DBReset.ADMIN_SECOND_PURCHASE_ID = id++;
		DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID = 1L;
		DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID = 2L;
		addPurchase(DBReset.ADMIN_SECOND_PURCHASE_ID, DBReset.ADMIN_ID, OffsetDateTime.of(2011, 4, 3, 9, 15, 0, 0, ZoneOffset.UTC),
				newItem(DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID, DBReset.WILSON_BALL_ID, 1, 45.30),
				newItem(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID, DBReset.THREAD_SEAL_TAPE_ID, 1, 2.67));

		dsl.alterSequence(SQ_PURCHASE).restartWith(id).execute();
		dsl.alterSequence(SQ_PURCHASEITEM).restartWith(3L).execute();
	}

	private static void addUser(long id, String userName, String password, String name, String roles) {
		var userRepo = UserRepository.BEAN.get();
		
		var user = new User()
				.withId(id)
				.withUserName(userName)
				.withName(name)
				.withRoles(roles);
		if (StringUtils.isNotBlank(password)) {
			user.withPassword(new BigInteger(md5().digest(password.getBytes(StandardCharsets.UTF_8))).toString(36));
		}
		userRepo.insert(user);
	}

	private static void addProduct(long id, String name, double price, String description, String imageResource) {
		var productRepo = ProductRepository.BEAN.get();
		
		var product = new Product()
				.withId(id)
				.withName(name)
				.withPrice(price)
				.withDescription(description);

		if (imageResource != null) {
			InputStream imageStream = DBReset.class.getResourceAsStream("/META-INF/" + imageResource);
			if (imageStream != null) {
				try (imageStream) {
					product.withImage(IOUtils.toByteArray(imageStream));
				} catch (IOException caught) {
					throw ExceptionUtils.asRuntimeException(caught);
				}
			}
		}

		productRepo.insert(product);
	}

	private static PurchaseItem newItem(long id, long productId, int amount, double price) {
		var item = new PurchaseItem()
				.withId(id)
				.withProduct(new Product()
						.withId(productId));
		item.withAmount(amount)
				.withPrice(price);
		return item;
	}

	private static void addPurchase(long id, long userId, OffsetDateTime buyDate, PurchaseItem... items) {
		var purchaseRepo = PurchaseRepository.BEAN.get();
		
		var purchase = new Purchase()
				.withId(id)
				.withUser(new User()
						.withId(userId));
		purchase.withBuyDate(buyDate)
				.withItems(List.of(items));
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
