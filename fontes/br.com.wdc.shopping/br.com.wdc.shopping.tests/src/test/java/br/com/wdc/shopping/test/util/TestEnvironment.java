package br.com.wdc.shopping.test.util;

import java.nio.file.Paths;
import java.sql.SQLException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.security.CryptoProvider;
import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.framework.persistence.transaction.RemoteTransactionCoordinatorImpl;
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
import br.com.wdc.shopping.persistence.rest.RemoteTransactions;
import br.com.wdc.shopping.persistence.rest.RepositoryApiRoutes;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.javalin.Javalin;

/**
 * Ambiente de teste reutilizável via {@code @ClassRule}. Pode operar em modo LOCAL (persistence direta) ou REST (via
 * Javalin embarcado).
 */
public class TestEnvironment extends ExternalResource {

    public enum Mode {
        LOCAL, REST
    }

    private final Mode mode;

    private final Defer cleanUp = new Defer();
    private AgroalDataSource datasource;
    private ScheduledExecutorForTest executor;
    private Javalin javalin;
    private OkHttpTransport transport;

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

    /** Transporte HTTP usado pelos repositórios no modo REST (para criar um TransactionService remoto no teste). */
    public OkHttpTransport transport() {
        return transport;
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
        ScheduledExecutor.BEAN.set(executor);
        cleanUp.push(() -> {
            executor.shutdown();
            executor = null;
            ScheduledExecutor.BEAN.set(null);
        });

        var dbName = mode == Mode.LOCAL ? "wedocode-shopping" : "wedocode-shopping-rest";
        var ds = AgroalDataSource.from(
                new AgroalDataSourceConfigurationSupplier()
                        .connectionPoolConfiguration(cp -> cp
                                .maxSize(10)
                                .minSize(1)
                                .initialSize(1)
                                .connectionFactoryConfiguration(cf -> cf
                                        .jdbcUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                                        .principal(new NamePrincipal("sa"))
                                        .credential(new SimplePassword("sa"))
                                        .connectionProviderClassName("org.h2.jdbcx.JdbcDataSource"))));
        datasource = ds;
        cleanUp.push(() -> {
            ds.close();
            datasource = null;
        });

        var basePath = Paths.get("work");
        ShoppingConfig.Internals.setBaseDir(basePath);
        ShoppingConfig.Internals.setConfigDir(basePath.resolve("config"));
        ShoppingConfig.Internals.setDataDir(basePath.resolve("data"));
        ShoppingConfig.Internals.setLogDir(basePath.resolve("log"));
        ShoppingConfig.Internals.setTempDir(basePath.resolve("temp"));

        // Infraestrutura de criptografia: os hosts reais (backend, Vaadin, SWT, TeaVM) a instalam no seu
        // composition root, e o ambiente de teste é um host como os outros. Sem ela, PasswordUtil recusa a
        // operação — e é dele que o login depende para conferir o resumo da senha.
        CryptoProvider.BEAN.set(new JceCryptoProvider());
        cleanUp.push(() -> CryptoProvider.BEAN.set(null));

        ShoppingRepositoryBootstrap.initialize(ds, cleanUp);

        if (mode == Mode.LOCAL) {
            userRepo = UserRepository.BEAN.get();
            productRepo = ProductRepository.BEAN.get();
            purchaseRepo = PurchaseRepository.BEAN.get();
            purchaseItemRepo = PurchaseItemRepository.BEAN.get();
        } else {
            // Coordenador de transação remota: o servidor REST de teste é um composition root próprio
            // (não passa pelo BusinessContext), então fia o coordenador aqui, como o backend faz.
            RemoteTransactions.COORDINATOR.set(new RemoteTransactionCoordinatorImpl(() -> ds));
            cleanUp.push(() -> RemoteTransactions.COORDINATOR.set(null));

            javalin = Javalin.create(config -> {
                config.http.maxRequestSize = 10_000_000L;
                RepositoryApiRoutes.configure(config, "");
            }).start(0);
            cleanUp.push(() -> {
                javalin.stop();
                javalin = null;
            });

            this.transport = new OkHttpTransport("http://localhost:" + javalin.port());
            userRepo = new HttpUserRepository(transport, new UserCodec());
            productRepo = new HttpProductRepository(transport, new ProductCodec());
            purchaseRepo = new HttpPurchaseRepository(transport, new PurchaseCodec());
            purchaseItemRepo = new HttpPurchaseItemRepository(transport, new PurchaseItemCodec());
        }
    }

    @Override
    protected void after() {
        cleanUp.run();
    }
}
