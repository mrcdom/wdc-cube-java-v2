package br.com.wdc.shopping.view.remote.javaclient.scenario.shopping;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.remote.javaclient.HostClient;
import br.com.wdc.shopping.view.remote.javaclient.scenario.ScenarioAssert;
import br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.presentation.BrowserPresenterClient;
import br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.presentation.CartPresenterClient;
import br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.presentation.HomePresenterClient;
import br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.presentation.LoginPresenterClient;
import br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.presentation.ProductPresenterClient;
import br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.presentation.ReceiptPresenterClient;
import br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.presentation.ShoppingRoutes;

/**
 * Cenário de integração: comprar produto.
 * <p>
 * Espelha {@code ShoppingWorkflowTest.testComprarProduto}, mas executa contra um Host real via protocolo Host/Shell —
 * sem mocks, sem contexto Spring, sem banco embutido.
 *
 * <h3>Pré-requisitos</h3>
 * <ul>
 * <li>Host rodando em {@code http://localhost:8080} (ou via argumento {@code args[0]})</li>
 * <li>Banco de dados no estado equivalente ao {@code DBReset}: usuário {@code admin/admin}, produtos com IDs
 * sequenciais a partir de 0</li>
 * </ul>
 *
 * <h3>Execução</h3>
 * 
 * <pre>
 *   mvn exec:java -pl ...remote.shell.java-client \
 *     -Dexec.mainClass=br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.ComprarProdutoScenario
 * </pre>
 */
public class ComprarProdutoScenario {

    private static final Logger LOG = LoggerFactory.getLogger(ComprarProdutoScenario.class);

    // Product IDs matching DBReset sequential insertion order (id starts at 0)
    private static final long BOLA_WILSON_ID = 1L;
    private static final long FITA_VEDA_ROSCA_ID = 2L;
    private static final long PEN_DRIVE2GB_ID = 3L;

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "http://localhost:8080";
        new ComprarProdutoScenario().run(host);
    }

    public void run(String host) throws Exception {
        LOG.info("=== Cenário: Comprar Produto | host={} ===", host);

        try (var client = HostClient.connect(host)) {
            doRun(client);
        }

        LOG.info("=== Cenário concluído com sucesso ===");
    }

    private void doRun(HostClient client) throws IOException, InterruptedException, TimeoutException {
        // Resetar base de dados para estado inicial conhecido (requer server.devMode=true)
        client.resetDb();

        // Consumir o push inicial assíncrono (sem requestId)
        client.awaitResponse();

        var root = BrowserPresenterClient.get(client).root();

        // -- [1] Login --
        LOG.info("[1] Login");
        client.navigate(ShoppingRoutes.open_login());

        var login = root.pageAs(LoginPresenterClient.class);
        login.onEnter("admin", "admin");

        // -- [2] Abrir produto Pen Drive 2GB --
        LOG.info("[2] Abrir Pen Drive 2GB (id={})", PEN_DRIVE2GB_ID);

        var home = root.pageAs(HomePresenterClient.class);
        home.productPanel().onOpenProduct(PEN_DRIVE2GB_ID);

        var product = home.contentViewAs(ProductPresenterClient.class);

        ScenarioAssert.assertNotNull("Produto deve ter sido selecionado", product.product());
        ScenarioAssert.assertEquals("Produto deve ser PEN_DRIVE2GB", PEN_DRIVE2GB_ID, product.productId());

        // -- [3] Adicionar ao carrinho (qty=1) --
        LOG.info("[3] Adicionar ao carrinho (qty=1)");
        product.onAddToCart(1);

        var cart = home.contentViewAs(CartPresenterClient.class);

        ScenarioAssert.assertEquals("Sem erros", 0, cart.errorCode());
        ScenarioAssert.assertEquals("Um item no carrinho", 1, cart.itemCount());
        ScenarioAssert.assertEquals("Quantidade deve ser 1", 1L, quantityOf(cart.items().get(0)));
        ScenarioAssert.assertEquals("ID no carrinho", PEN_DRIVE2GB_ID, idOf(cart.items().get(0)));

        // -- [4] Tentar modificar quantidade para 0 (inválido) --
        LOG.info("[4] Modificar quantidade para 0 (inválido)");
        cart.onModifyQuantity(PEN_DRIVE2GB_ID, 0);

        cart = home.contentViewAs(CartPresenterClient.class);
        ScenarioAssert.assertEquals("errorCode deve indicar quantidade inválida", 1, cart.errorCode());

        // -- [5] Modificar quantidade para 2 (válido) --
        LOG.info("[5] Modificar quantidade para 2");
        cart.onModifyQuantity(PEN_DRIVE2GB_ID, 2);

        cart = home.contentViewAs(CartPresenterClient.class);
        ScenarioAssert.assertEquals("Sem erros", 0, cart.errorCode());
        ScenarioAssert.assertEquals("Quantidade deve ser 2", 2L, quantityOf(cart.items().get(0)));

        // -- [6] Produto ID inválido --
        LOG.info("[6] Modificar quantidade com ID inválido");
        cart.onModifyQuantity(Long.MIN_VALUE, 2);

        cart = home.contentViewAs(CartPresenterClient.class);
        ScenarioAssert.assertEquals("errorCode deve indicar produto não encontrado", 2, cart.errorCode());

        // -- [7] Voltar para produtos e abrir Bola Wilson --
        LOG.info("[7] Voltar para produtos → abrir Bola Wilson (id={})", BOLA_WILSON_ID);
        cart.onOpenProducts();

        home.productPanel().onOpenProduct(BOLA_WILSON_ID);

        product = home.contentViewAs(ProductPresenterClient.class);
        ScenarioAssert.assertEquals("Produto deve ser BOLA_WILSON", BOLA_WILSON_ID, product.productId());

        // -- [8] Voltar para produtos e abrir Fita Veda Rosca --
        LOG.info("[8] Voltar para produtos → abrir Fita Veda Rosca (id={})", FITA_VEDA_ROSCA_ID);
        product.onOpenProducts();

        home.productPanel().onOpenProduct(FITA_VEDA_ROSCA_ID);

        product = home.contentViewAs(ProductPresenterClient.class);
        ScenarioAssert.assertEquals("Produto deve ser FITA_VEDA_ROSCA", FITA_VEDA_ROSCA_ID, product.productId());

        // -- [9] Adicionar Fita Veda Rosca ao carrinho --
        LOG.info("[9] Adicionar Fita Veda Rosca ao carrinho");
        product.onAddToCart(1);

        cart = home.contentViewAs(CartPresenterClient.class);
        ScenarioAssert.assertEquals("Dois itens no carrinho", 2, cart.itemCount());
        ScenarioAssert.assertEquals("Segundo item com quantidade 1", 1L, quantityOf(cart.items().get(1)));

        // -- [10] Comprar --
        LOG.info("[10] Comprar");
        cart.onBuy();

        var receipt = home.contentViewAs(ReceiptPresenterClient.class);
        ScenarioAssert.assertTrue("Recibo deve estar marcado como novo", receipt.notifySuccess());
        ScenarioAssert.assertNotNull("Recibo não deve ser nulo", receipt.receipt());
        ScenarioAssert.assertEquals("Recibo deve ter 2 itens", 2, itemsOf(receipt.receipt()).size());

        // -- [11] Voltar para produtos --
        LOG.info("[11] Voltar para produtos a partir do recibo");
        receipt.onOpenProducts();

        // Home deve estar ativo sem sub-view (contentViewId deve ser null)
        ScenarioAssert.assertNull("Nenhuma sub-view deveria estar ativa no Home",
                home.contentViewId());
    }

    // :: Map accessors for cart/receipt item fields

    private static long quantityOf(Map<String, Object> item) {
        return ((Number) item.get("quantity")).longValue();
    }

    private static long idOf(Map<String, Object> item) {
        return ((Number) item.get("id")).longValue();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> itemsOf(Map<String, Object> receiptMap) {
        var v = receiptMap.get("items");
        return v instanceof List<?> l
                ? (List<Object>) l
                : Collections.emptyList();
    }
}
