package br.com.wdc.store.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.store.domain.StoreTransactions;
import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.persistence.impl.repository.ProductImageCache;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.TestEnvironment;

/**
 * The product images changed here are told (after the commit), for the other backend instances to drop theirs; the
 * ones dropped because another instance changed them are not told again.
 */
public class ProductImageChangesTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

    @Rule
    public ExternalResource resetDb = new ResetDatabaseRule(env);

    private final List<Long> told = Collections.synchronizedList(new ArrayList<>());
    private Registration registration;

    @Before
    public void listen() {
        registration = ProductImageCache.onChanged(told::add);
    }

    @After
    public void stopListening() {
        registration.remove();
    }

    @Test
    public void anImageChangedHere_isTold_afterTheCommit() {
        var products = ProductRepository.BEAN.get();
        var image = new byte[] { 4, 5, 6 };

        StoreTransactions.BEAN.get().required(tx -> {
            products.updateImage(DBReset.THREAD_SEAL_TAPE_ID, image);
            assertEquals("not before the commit", List.of(), told);
        });

        assertEquals(List.of(DBReset.THREAD_SEAL_TAPE_ID), told);
        assertArrayEquals(image, products.fetchImage(DBReset.THREAD_SEAL_TAPE_ID));
    }

    @Test
    public void anImageChangedElsewhere_isDropped_withoutTellingAgain() {
        var products = ProductRepository.BEAN.get();
        products.fetchImage(DBReset.THREAD_SEAL_TAPE_ID);

        ProductImageCache.evict(DBReset.THREAD_SEAL_TAPE_ID);
        ProductImageCache.clear();

        assertEquals(List.of(), told);
    }
}
