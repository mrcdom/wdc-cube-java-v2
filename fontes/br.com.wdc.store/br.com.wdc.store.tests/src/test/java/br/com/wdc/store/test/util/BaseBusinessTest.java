package br.com.wdc.store.test.util;

import java.nio.file.Paths;
import java.sql.SQLException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.security.CryptoProvider;
import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.store.domain.StoreConfig;
import br.com.wdc.store.persistence.impl.StoreRepositoryBootstrap;
import br.com.wdc.store.scripts.sgbd.DBCreate;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;

@SuppressWarnings("java:S2187") // base class — tests are in subclasses
public class BaseBusinessTest {

    private static AgroalDataSource datasource;

    protected static ScheduledExecutorForTest executor;

    protected static Defer cleanUp = new Defer();

    @BeforeClass
    public static void beforeClass() throws Exception {
        executor = new ScheduledExecutorForTestAsync();
        cleanUp.push(executor::shutdown);

        var ds = AgroalDataSource.from(
                new AgroalDataSourceConfigurationSupplier()
                        .connectionPoolConfiguration(cp -> cp
                                .maxSize(10)
                                .minSize(1)
                                .initialSize(1)
                                .connectionFactoryConfiguration(cf -> cf
                                        .jdbcUrl(
                                                "jdbc:h2:mem:wedocode-store;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                                        .principal(new NamePrincipal("sa"))
                                        .credential(new SimplePassword("sa"))
                                        .connectionProviderClassName("org.h2.jdbcx.JdbcDataSource"))));
        BaseBusinessTest.datasource = ds;
        cleanUp.push(() -> {
            ds.close();
            BaseBusinessTest.datasource = null;
        });

        var basePath = Paths.get("work");

        StoreConfig.Internals.setBaseDir(basePath);
        StoreConfig.Internals.setConfigDir(basePath.resolve("config"));
        StoreConfig.Internals.setDataDir(basePath.resolve("data"));
        StoreConfig.Internals.setLogDir(basePath.resolve("log"));
        StoreConfig.Internals.setTempDir(basePath.resolve("temp"));

        ScheduledExecutor.BEAN.set(executor);

        // Cryptography infrastructure: the real hosts install it in their composition root, and this environment is
        // a host like the others. Without it, PasswordUtil refuses the operation — and login depends on it.
        CryptoProvider.BEAN.set(new JceCryptoProvider());
        cleanUp.push(() -> CryptoProvider.BEAN.set(null));

        StoreRepositoryBootstrap.initialize(ds, cleanUp);
    }

    @AfterClass
    public static void afterClass() {
        cleanUp.run();
    }

    /*
     * Instance
     */

    @Before
    public void before() {
        try (var connection = BaseBusinessTest.datasource.getConnection()) {
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
