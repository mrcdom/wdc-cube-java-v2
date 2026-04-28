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
import br.com.wdc.shopping.business.impl.RepositoryBootstrap;
import br.com.wdc.shopping.business.impl.sgbd.ddl.scripts.DBCreate;
import br.com.wdc.shopping.business.shared.ShoppingConfig;

public class BaseBusinessTest {

	private static BasicDataSource datasource;

	protected static ScheduledExecutorForTest executor;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// executor = new ScheduledExecutorForTestSyncDirect();
		// executor = new ScheduledExecutorForTestSyncDelayed();
		executor = new ScheduledExecutorForTestAsync();

		final BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName("org.h2.jdbcx.JdbcDataSource");
		ds.setUrl("jdbc:h2:mem:wedocode-shopping;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
		ds.setUsername("sa");
		ds.setPassword("sa");
		ds.setInitialSize(1);
		ds.setMaxActive(10);
		ds.setMaxIdle(5);
		ds.setValidationQuery("SELECT 1 FROM DUAL");
		BaseBusinessTest.datasource = ds;

		var basePath = Paths.get("work");

		ShoppingConfig.Internals.setBaseDir(basePath);
		ShoppingConfig.Internals.setConfigDir(basePath.resolve("config"));
		ShoppingConfig.Internals.setDataDir(basePath.resolve("data"));
		ShoppingConfig.Internals.setLogDir(basePath.resolve("log"));
		ShoppingConfig.Internals.setTempDir(basePath.resolve("temp"));

		SqlDataSource.BEAN.set(new SqlDataSourceDelegate(ds));
		ScheduledExecutor.BEAN.set(executor);

		RepositoryBootstrap.initialize();
	}

	@AfterClass
	public static void afterClass() throws SQLException {
	    RepositoryBootstrap.release();
		BaseBusinessTest.datasource.close();
		BaseBusinessTest.datasource = null;
		executor.shutdown();
	}

	/*
	 * Instance
	 */

	@Before
	public void before() {
		try (var connection = BaseBusinessTest.datasource.getConnection()) {
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
