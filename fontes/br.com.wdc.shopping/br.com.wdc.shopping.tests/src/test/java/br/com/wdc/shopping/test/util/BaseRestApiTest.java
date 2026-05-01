package br.com.wdc.shopping.test.util;

import java.nio.file.Paths;
import java.sql.SQLException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tomcat.dbcp.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.sql.SqlDataSource;
import br.com.wdc.framework.commons.sql.SqlDataSourceDelegate;
import br.com.wdc.shopping.api.RepositoryApiRoutes;
import br.com.wdc.shopping.api.client.RestConfig;
import br.com.wdc.shopping.api.client.RestProductRepository;
import br.com.wdc.shopping.api.client.RestPurchaseItemRepository;
import br.com.wdc.shopping.api.client.RestPurchaseRepository;
import br.com.wdc.shopping.api.client.RestUserRepository;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.persistence.RepositoryBootstrap;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;
import io.javalin.Javalin;

/**
 * Base para testes de integração do api-client.
 * Sobe um servidor Javalin embarcado com a API REST real (usando H2 em memória)
 * e expõe instâncias REST client para os testes usarem diretamente.
 *
 * Os BEANs estáticos ficam apontando para a implementação de persistência local
 * (usada pelo servidor). Os testes usam os campos protegidos (REST client instances).
 */
public class BaseRestApiTest {

	private static BasicDataSource datasource;
	private static Javalin javalin;

	protected static ScheduledExecutorForTest executor;

	protected static UserRepository userRepo;
	protected static ProductRepository productRepo;
	protected static PurchaseRepository purchaseRepo;
	protected static PurchaseItemRepository purchaseItemRepo;

	@BeforeClass
	public static void beforeClass() throws Exception {
		executor = new ScheduledExecutorForTestAsync();

		// Datasource H2 em memória
		final BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName("org.h2.jdbcx.JdbcDataSource");
		ds.setUrl("jdbc:h2:mem:wedocode-shopping-rest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
		ds.setUsername("sa");
		ds.setPassword("sa");
		ds.setInitialSize(1);
		ds.setMaxActive(10);
		ds.setMaxIdle(5);
		ds.setValidationQuery("SELECT 1 FROM DUAL");
		datasource = ds;

		var basePath = Paths.get("work");
		ShoppingConfig.Internals.setBaseDir(basePath);
		ShoppingConfig.Internals.setConfigDir(basePath.resolve("config"));
		ShoppingConfig.Internals.setDataDir(basePath.resolve("data"));
		ShoppingConfig.Internals.setLogDir(basePath.resolve("log"));
		ShoppingConfig.Internals.setTempDir(basePath.resolve("temp"));

		SqlDataSource.BEAN.set(new SqlDataSourceDelegate(ds));
		ScheduledExecutor.BEAN.set(executor);

		// Inicializa repositórios locais (persistence) — ficam nos BEANs para o servidor usar
		RepositoryBootstrap.initialize();

		// Sobe o servidor Javalin embarcado (usa os BEANs → persistence local)
		javalin = Javalin.create(config -> {
			config.http.maxRequestSize = 10_000_000L; // 10MB
			RepositoryApiRoutes.configure(config);
		}).start(0);

		int actualPort = javalin.port();

		// Cria instâncias REST client (não sobrescrevem os BEANs)
		var restConfig = new RestConfig("http://localhost:" + actualPort);
		userRepo = new RestUserRepository(restConfig);
		productRepo = new RestProductRepository(restConfig);
		purchaseRepo = new RestPurchaseRepository(restConfig);
		purchaseItemRepo = new RestPurchaseItemRepository(restConfig);
	}

	@AfterClass
	public static void afterClass() throws SQLException {
		if (javalin != null) {
			javalin.stop();
			javalin = null;
		}

		RepositoryBootstrap.release();

		datasource.close();
		datasource = null;
		executor.shutdown();
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
