package br.com.wdc.store.view.remote.shell.probe.store;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.store.view.remote.shell.probe.ScenarioAssert;
import br.com.wdc.store.view.remote.shell.probe.store.presentation.BrowserPresenterClient;
import br.com.wdc.store.view.remote.shell.probe.store.presentation.CartPresenterClient;
import br.com.wdc.store.view.remote.shell.probe.store.presentation.HomePresenterClient;
import br.com.wdc.store.view.remote.shell.probe.store.presentation.LoginPresenterClient;
import br.com.wdc.store.view.remote.shell.probe.store.presentation.ProductPresenterClient;
import br.com.wdc.store.view.remote.shell.probe.store.presentation.ReceiptPresenterClient;
import br.com.wdc.store.view.remote.shell.probe.store.presentation.StoreRoutes;

/**
 * Integration scenario: buy a product.
 * <p>
 * Mirrors {@code StoreWorkflowTest.testBuyProduct}, but runs against a real Host over the Host/Shell protocol —
 * no mocks, no Spring context, no embedded database.
 *
 * <h3>Prerequisites</h3>
 * <ul>
 * <li>Host running at {@code http://localhost:8080} (or given as argument {@code args[0]})</li>
 * <li>Database in the state produced by {@code DBReset}: user {@code admin/admin}, products with sequential IDs
 * starting at 0</li>
 * </ul>
 *
 * <h3>Running</h3>
 * 
 * <pre>
 *   mvn exec:java -pl ...remote.shell.probe-client \
 *     -Dexec.mainClass=br.com.wdc.store.view.remote.javaclient.scenario.store.BuyProductScenario
 * </pre>
 */
public class BuyProductScenario {

    private static final Logger LOG = LoggerFactory.getLogger(BuyProductScenario.class);

    // Product IDs matching DBReset sequential insertion order (id starts at 0)
    private static final long WILSON_BALL_ID = 1L;
    private static final long THREAD_SEAL_TAPE_ID = 2L;
    private static final long FLASH_DRIVE_2GB_ID = 3L;

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "http://localhost:8080";
        new BuyProductScenario().run(host);
    }

    public void run(String host) throws Exception {
        LOG.info("=== Scenario: Buy Product | host={} ===", host);

        try (var client = HostClient.connect(host)) {
            doRun(client);
        }

        LOG.info("=== Scenario completed successfully ===");
    }

    private void doRun(HostClient client) throws IOException, InterruptedException, TimeoutException {
        // Reset the database to a known initial state (requires server.devMode=true)
        client.resetDb();

        // Consume the initial asynchronous push (no requestId)
        client.awaitResponse();

        var root = BrowserPresenterClient.get(client).root();

        // -- [1] Login --
        LOG.info("[1] Login");
        client.navigate(StoreRoutes.open_login());

        var login = root.pageAs(LoginPresenterClient.class);
        login.onEnter("admin", "admin");

        // -- [2] Open product 2GB Flash Drive --
        LOG.info("[2] Open 2GB Flash Drive (id={})", FLASH_DRIVE_2GB_ID);

        var home = root.pageAs(HomePresenterClient.class);
        home.productPanel().onOpenProduct(FLASH_DRIVE_2GB_ID);

        var product = home.contentViewAs(ProductPresenterClient.class);

        ScenarioAssert.assertNotNull("Product should have been selected", product.product());
        ScenarioAssert.assertEquals("Product should be FLASH_DRIVE_2GB", FLASH_DRIVE_2GB_ID, product.productId());

        // -- [3] Add to cart (qty=1) --
        LOG.info("[3] Add to cart (qty=1)");
        product.onAddToCart(1);

        var cart = home.contentViewAs(CartPresenterClient.class);

        ScenarioAssert.assertEquals("No errors", 0, cart.errorCode());
        ScenarioAssert.assertEquals("One item in the cart", 1, cart.itemCount());
        var flashDrive = cart.rows().get(0);
        ScenarioAssert.assertEquals("Quantity should be 1", 1L, flashDrive.quantity());
        ScenarioAssert.assertEquals("ID in the cart", FLASH_DRIVE_2GB_ID, flashDrive.id());

        // -- [4] Try to change quantity to 0 (invalid) --
        LOG.info("[4] Change quantity to 0 (invalid)");
        flashDrive.onModifyQuantity(0);

        cart = home.contentViewAs(CartPresenterClient.class);
        ScenarioAssert.assertEquals("errorCode should indicate an invalid quantity", 1, cart.errorCode());

        // -- [5] Change quantity to 2 (valid) --
        LOG.info("[5] Change quantity to 2");
        flashDrive.onModifyQuantity(2);

        cart = home.contentViewAs(CartPresenterClient.class);
        ScenarioAssert.assertEquals("No errors", 0, cart.errorCode());
        ScenarioAssert.assertEquals("Quantity should be 2", 2L, cart.rows().get(0).quantity());

        // -- [6] The row is reused: the same view, with the new quantity --
        LOG.info("[6] Row reused");
        ScenarioAssert.assertTrue("Same row view", flashDrive.instanceId().equals(cart.rows().get(0).instanceId()));

        // -- [7] Back to products and open Wilson Ball --
        LOG.info("[7] Back to products → open Wilson Ball (id={})", WILSON_BALL_ID);
        cart.onOpenProducts();

        home.productPanel().onOpenProduct(WILSON_BALL_ID);

        product = home.contentViewAs(ProductPresenterClient.class);
        ScenarioAssert.assertEquals("Product should be WILSON_BALL", WILSON_BALL_ID, product.productId());

        // -- [8] Back to products and open Thread Seal Tape --
        LOG.info("[8] Back to products → open Thread Seal Tape (id={})", THREAD_SEAL_TAPE_ID);
        product.onOpenProducts();

        home.productPanel().onOpenProduct(THREAD_SEAL_TAPE_ID);

        product = home.contentViewAs(ProductPresenterClient.class);
        ScenarioAssert.assertEquals("Product should be THREAD_SEAL_TAPE", THREAD_SEAL_TAPE_ID, product.productId());

        // -- [9] Add Thread Seal Tape to the cart --
        LOG.info("[9] Add Thread Seal Tape to the cart");
        product.onAddToCart(1);

        cart = home.contentViewAs(CartPresenterClient.class);
        ScenarioAssert.assertEquals("Two items in the cart", 2, cart.itemCount());
        ScenarioAssert.assertEquals("Second item with quantity 1", 1L, cart.rows().get(1).quantity());

        // -- [10] Buy --
        LOG.info("[10] Buy");
        cart.onBuy();

        var receipt = home.contentViewAs(ReceiptPresenterClient.class);
        ScenarioAssert.assertTrue("Receipt should be flagged as new", receipt.notifySuccess());
        ScenarioAssert.assertNotNull("Receipt should not be null", receipt.receipt());
        ScenarioAssert.assertEquals("Receipt should have 2 items", 2, itemsOf(receipt.receipt()).size());

        // -- [11] Back to products --
        LOG.info("[11] Back to products from the receipt");
        receipt.onOpenProducts();

        // Home should be active with no sub-view (contentViewId should be null)
        ScenarioAssert.assertNull("No sub-view should be active in Home",
                home.contentViewId());
    }

    // :: Map accessors for cart/receipt item fields

    @SuppressWarnings("unchecked")
    private static List<Object> itemsOf(Map<String, Object> receiptMap) {
        var v = receiptMap.get("items");
        return v instanceof List<?> l
                ? (List<Object>) l
                : Collections.emptyList();
    }
}
