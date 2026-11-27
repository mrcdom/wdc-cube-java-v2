package br.com.wdc.store.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import com.google.gson.JsonParser;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.TransactionConflictException;
import br.com.wdc.store.domain.product.Product;
import br.com.wdc.store.domain.purchase.Purchase;
import br.com.wdc.store.domain.purchaseitem.PurchaseItem;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.RestSession;
import br.com.wdc.store.test.util.TestEnvironment;

/**
 * With security on, a remote transaction belongs to the user's client context ({@code X-Client-Id}: a tab, an app
 * instance), not to the user alone: a second tab of the same user writes on its own while the first has a transaction
 * open, and cannot finish it.
 */
public class RemoteTransactionOwnerRestTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST).withSecurity();

    @Rule
    public ExternalResource resetDb = new ResetDatabaseRule(env);

    /** Two clients of jdoe: each transport has its own X-Client-Id. */
    private RestSession tabA;
    private RestSession tabB;

    /** After the database is reset (the rule): a sign-in needs the seeded user. */
    @Before
    public void signInTwice() {
        tabA = RestSession.signIn(env, "jdoe");
        tabB = RestSession.signIn(env, "jdoe");
    }

    private static String begin(RestSession tab) {
        var body = tab.transport()
                .postJson("/api/tx/begin", "{}");
        return JsonParser.parseString(body)
                .getAsJsonObject()
                .get("txId")
                .getAsString();
    }

    private static Purchase coffeeMaker() {
        return new Purchase()
                .withUser(new User()
                        .withId(DBReset.JOHN_DOE_ID))
                .withBuyDate(OffsetDateTime.now())
                .withItems(new ArrayList<>(List.of(new PurchaseItem()
                        .withProduct(new Product()
                                .withId(DBReset.COFFEE_MAKER_ID))
                        .withAmount(1))));
    }

    @Test
    public void anotherTabOfTheSameUser_writesOnItsOwn_whileTheFirstHasATransactionOpen() {
        begin(tabA);

        var purchase = coffeeMaker();
        assertTrue(tabB.purchases()
                .insert(purchase));
        assertNotNull(purchase.id());
    }

    @Test
    public void theTabWithTheTransactionOpen_isStillGuarded() {
        begin(tabA);

        assertThrows("its own write outside its transaction: 409", TransactionConflictException.class,
                () -> tabA.purchases()
                        .insert(coffeeMaker()));
    }

    @Test
    public void anotherTab_cannotFinishTheTransaction() {
        var txId = begin(tabA);

        tabB.transport()
                .setTransactionIdSupplier(() -> txId);
        assertThrows(AccessDeniedException.class, () -> tabB.transport()
                .postJson("/api/tx/commit", "{}"));

        tabA.transport()
                .setTransactionIdSupplier(() -> txId);
        tabA.transport()
                .postJson("/api/tx/rollback", "{}");
    }
}
