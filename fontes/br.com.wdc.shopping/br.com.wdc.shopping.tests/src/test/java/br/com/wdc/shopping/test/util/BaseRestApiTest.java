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
import br.com.wdc.shopping.domain.codec.ProductModelCodec;
import br.com.wdc.shopping.domain.codec.PurchaseItemModelCodec;
import br.com.wdc.shopping.domain.codec.PurchaseModelCodec;
import br.com.wdc.shopping.domain.codec.UserModelCodec;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;
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
 * Base para testes de integração do api-client.
 * Sobe um servidor Javalin embarcado com a API REST real (usando H2 em memória)
 * e expõe instâncias REST client para os testes usarem diretamente.
 *
 * Os BEANs estáticos ficam apontando para a implementação de persistência local
 * (usada pelo servidor). Os testes usam os campos protegidos (REST client instances).
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

		// Datasource H2 em memória
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

		// Inicializa repositórios locais (persistence) — ficam nos BEANs para o servidor usar
		ShoppingRepositoryBootstrap.initialize(ds, cleanUp);

		// Sobe o servidor Javalin embarcado (usa os BEANs → persistence local)
		javalin = Javalin.create(config -> {
			config.http.maxRequestSize = 10_000_000L; // 10MB
			RepositoryApiRoutes.configure(config, "");
		}).start(0);
		cleanUp.push(() -> {
		    javalin.stop();
		    javalin = null;
		});

		int actualPort = javalin.port();

		// Cria instâncias REST client (não sobrescrevem os BEANs)
		var transport = new OkHttpTransport("http://localhost:" + actualPort);
		userRepo = new HttpUserRepository(transport, new UserModelCodec());
		productRepo = new HttpProductRepository(transport, new ProductModelCodec());
		purchaseRepo = new HttpPurchaseRepository(transport, new PurchaseModelCodec());
		purchaseItemRepo = new HttpPurchaseItemRepository(transport, new PurchaseItemModelCodec());

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
			new DBCreate().withConnection(connection).withReset().run();
		} catch (SQLException caught) {
			throw ExceptionUtils.asRuntimeException(caught);
		}
	}

	@After
	public void after() {
		// NOOP
	}
}
