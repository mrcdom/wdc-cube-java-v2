package br.com.wdc.store.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.store.persistence.client.OkHttpTransport;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.RestSession;
import br.com.wdc.store.test.util.TestEnvironment;

/** With security on: anyone reads a product image; only who may write the image field changes it. */
public class ProductImageRestTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST).withSecurity();

    @Rule
    public ExternalResource resetDb = new ResetDatabaseRule(env);

    @Test
    public void anyoneReadsAnImage() {
        var anonymous = new OkHttpTransport(env.baseUrl());

        assertTrue(anonymous.getBytes("/api/repo/product/" + DBReset.THREAD_SEAL_TAPE_ID + "/image").length > 0);
    }

    @Test
    public void anAdminChangesAnImage() {
        var admin = RestSession.signIn(env, "admin");
        var image = new byte[] { 1, 2, 3 };

        assertTrue(admin.products().updateImage(DBReset.THREAD_SEAL_TAPE_ID, image));

        assertArrayEquals(image, admin.products().fetchImage(DBReset.THREAD_SEAL_TAPE_ID));
    }

    @Test
    public void aCustomerCannotChangeAnImage() {
        var jdoe = RestSession.signIn(env, "jdoe");

        try {
            jdoe.products().updateImage(DBReset.THREAD_SEAL_TAPE_ID, new byte[] { 1, 2, 3 });
            fail("a customer may not write the image field");
        } catch (AccessDeniedException expected) {
            // expected: 403
        }
    }

    @Test
    public void anAnonymousClientCannotChangeAnImage() {
        var anonymous = new OkHttpTransport(env.baseUrl());

        try {
            anonymous.putBytes("/api/repo/product/" + DBReset.THREAD_SEAL_TAPE_ID + "/image", new byte[] { 1, 2, 3 });
            fail("changing an image needs a signed-in user");
        } catch (RuntimeException expected) {
            // expected: 401
        }
    }
}
