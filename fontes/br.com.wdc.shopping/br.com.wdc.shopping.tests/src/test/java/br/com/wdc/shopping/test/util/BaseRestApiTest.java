package br.com.wdc.shopping.test.util;

import java.nio.file.Paths;
import java.sql.SQLException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.domain.product.ProductCodec;
import br.com.wdc.shopping.domain.product.ProductRepository;
import br.com.wdc.shopping.domain.purchase.PurchaseCodec;
import br.com.wdc.shopping.domain.purchase.PurchaseRepository;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCodec;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.shopping.domain.user.UserCodec;
import br.com.wdc.shopping.domain.user.UserRepository;
import br.com.wdc.shopping.persistence.client.HttpProductRepository;
import br.com.wdc.shopping.persistence.client.HttpPurchaseItemRepository;
import br.com.wdc.shopping.persistence.client.HttpPurchaseRepository;
import br.com.wdc.shopping.persistence.client.HttpUserRepository;
import br.com.wdc.shopping.persistence.client.OkHttpTransport;
import br.com.wdc.shopping.persistence.impl.ShoppingRepositoryBootstrap;
import br.com.wdc.shopping.persistence.rest.RepositoryApiRoutes;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.javalin.Javalin;

/**
 * Base for api-client integration tests.
 * Starts an embedded Javalin server with the real REST API (using in-memory H2)
 * and exposes REST client instances for the tests to use directly.
 *
 * The static BEANs point to the local persistence implementation
 * (used by the server). The tests use the protected fields (REST client instances).
 */
@SuppressWarnings("java:S2187") // base class — tests are in subclasses
public class BaseRestApiTest {

    private static Defer cleanUp = new Defer();
	private static AgroalDataSource datasource;
	private static Javalin javalin;
	protected static ScheduledExecutorForTest executor;
	protected static UserRepository userRepo;
	protected static ProductRepository productRepo;
	protected static PurchaseRepository purchaseRepo;
	protected static PurchaseItemRepository purchaseItemRepo;

	@BeforeClass
	public static void beforeClass() throws Exception {
		executor = new ScheduledExecutorForTestAsync();
		cleanUp.push(executor::shutdown);

		ScheduledExecutor.BEAN.set(executor);
        cleanUp.push(() -> ScheduledExecutor.BEAN.set(null));

		// In-memory H2 datasource
		var ds = AgroalDataSource.from(
				new AgroalDataSourceConfigurationSupplier()
						.connectionPoolConfiguration(cp -> cp
								.maxSize(10)
								.minSize(1)
								.initialSize(1)
								.connectionFactoryConfiguration(cf -> cf
										.jdbcUrl("jdbc:h2:mem:wedocode-shopping-rest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
										.principal(new NamePrincipal("sa"))
										.credential(new SimplePassword("sa"))
										.connectionProviderClassName("org.h2.jdbcx.JdbcDataSource"))));
		datasource = ds;
		cleanUp.push(() -> {
            datasource = null;
		    ds.close();
		});

		var basePath = Paths.get("work");
		ShoppingConfig.Internals.setBaseDir(basePath);
		ShoppingConfig.Internals.setConfigDir(basePath.resolve("config"));
		ShoppingConfig.Internals.setDataDir(basePath.resolve("data"));
		ShoppingConfig.Internals.setLogDir(basePath.resolve("log"));
		ShoppingConfig.Internals.setTempDir(basePath.resolve("temp"));

		// Initializes the local repositories (persistence) — they stay in the BEANs for the server to use
		ShoppingRepositoryBootstrap.initialize(ds, cleanUp);

		// Starts the embedded Javalin server (uses the BEANs → local persistence)
		javalin = Javalin.create(config -> {
			config.http.maxRequestSize = 10_000_000L; // 10MB
			RepositoryApiRoutes.configure(config, "");
		}).start(0);
		cleanUp.push(() -> {
		    javalin.stop();
		    javalin = null;
		});

		int actualPort = javalin.port();

		// Creates REST client instances (they do not overwrite the BEANs)
		var transport = new OkHttpTransport("http://localhost:" + actualPort);
		userRepo = new HttpUserRepository(transport, new UserCodec());
		productRepo = new HttpProductRepository(transport, new ProductCodec());
		purchaseRepo = new HttpPurchaseRepository(transport, new PurchaseCodec());
		purchaseItemRepo = new HttpPurchaseItemRepository(transport, new PurchaseItemCodec());

		cleanUp.push(() -> {
		    userRepo = null;
		    productRepo = null;
		    purchaseRepo = null;
		    purchaseItemRepo = null;
		});
	}

	@AfterClass
	public static void afterClass() {
	    cleanUp.run();
	}

	@Before
	public void before() {
		try (var connection = datasource.getConnection()) {
			new DBCreate()
					.withConnection(connection)
					.withReset()
					.run();
		} catch (SQLException caught) {
			throw ExceptionUtils.asRuntimeException(caught);
		}
	}

	@After
	public void after() {
		// NOOP
	}
}
