package br.com.wdc.shopping.test.util;

import java.nio.file.Paths;
import java.sql.SQLException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tomcat.dbcp.dbcp.BasicDataSource;
import org.junit.rules.ExternalResource;

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
 * Ambiente de teste reutilizável via {@code @ClassRule}.
 * Pode operar em modo LOCAL (persistence direta) ou REST (via Javalin embarcado).
 */
public class TestEnvironment extends ExternalResource {

	public enum Mode { LOCAL, REST }

	private final Mode mode;

	private BasicDataSource datasource;
	private ScheduledExecutorForTest executor;
	private Javalin javalin;

	private UserRepository userRepo;
	private ProductRepository productRepo;
	private PurchaseRepository purchaseRepo;
	private PurchaseItemRepository purchaseItemRepo;

	public TestEnvironment(Mode mode) {
		this.mode = mode;
	}

	// :: Accessors

	public UserRepository userRepo() {
		return userRepo;
	}

	public ProductRepository productRepo() {
		return productRepo;
	}

	public PurchaseRepository purchaseRepo() {
		return purchaseRepo;
	}

	public PurchaseItemRepository purchaseItemRepo() {
		return purchaseItemRepo;
	}

	public void resetDatabase() {
		try (var connection = datasource.getConnection()) {
			new DBCreate().withConnection(connection).withReset().run();
		} catch (SQLException caught) {
			throw ExceptionUtils.asRuntimeException(caught);
		}
	}

	// :: ExternalResource lifecycle

	@Override
	protected void before() throws Throwable {
		executor = new ScheduledExecutorForTestAsync();

		var dbName = mode == Mode.LOCAL ? "wedocode-shopping" : "wedocode-shopping-rest";
		var ds = new BasicDataSource();
		ds.setDriverClassName("org.h2.jdbcx.JdbcDataSource");
		ds.setUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
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

		RepositoryBootstrap.initialize();

		if (mode == Mode.LOCAL) {
			userRepo = UserRepository.BEAN.get();
			productRepo = ProductRepository.BEAN.get();
			purchaseRepo = PurchaseRepository.BEAN.get();
			purchaseItemRepo = PurchaseItemRepository.BEAN.get();
		} else {
			javalin = Javalin.create(config -> {
				config.http.maxRequestSize = 10_000_000L;
				RepositoryApiRoutes.configure(config);
			}).start(0);

			var restConfig = new RestConfig("http://localhost:" + javalin.port());
			userRepo = new RestUserRepository(restConfig);
			productRepo = new RestProductRepository(restConfig);
			purchaseRepo = new RestPurchaseRepository(restConfig);
			purchaseItemRepo = new RestPurchaseItemRepository(restConfig);
		}
	}

	@Override
	protected void after() {
		if (javalin != null) {
			javalin.stop();
			javalin = null;
		}

		RepositoryBootstrap.release();

		try {
			datasource.close();
		} catch (SQLException e) {
			throw ExceptionUtils.asRuntimeException(e);
		}
		datasource = null;
		executor.shutdown();
	}
}
