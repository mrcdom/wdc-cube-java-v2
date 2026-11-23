package br.com.wdc.shopping.test;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeKeys;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootKeys;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptKeys;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ProductViewMock;
import br.com.wdc.shopping.test.util.BasePresentationTest;

/**
 * The keys classes: unique prefixes, one keys class per place with the class tree as the place tree, getters that never
 * trust the address, and what a bad address leads to.
 */
public class PlaceKeysTest extends BasePresentationTest {

    private static final Path PRESENTATION_SOURCES = Path.of("../br.com.wdc.shopping.presentation/src/main/java");

    /**
     * The keys classes (under {@code presenter}; {@code StorageKeys} is another thing), found by their source files: a
     * scan of the classpath would not see the ones never loaded.
     */
    private static List<Class<?>> keysClasses() throws IOException {
        try (var files = Files.walk(PRESENTATION_SOURCES)) {
            return files.filter(f -> f.getFileName().toString().endsWith("Keys.java") && f.toString().contains("/presenter/"))
                    .<Class<?>> map(f -> {
                var name = PRESENTATION_SOURCES.relativize(f).toString().replace('/', '.').replace(".java", "");
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException caught) {
                    throw new AssertionError(caught);
                }
            }).toList();
        }
    }

    private static Object staticField(Class<?> cls, String name) {
        try {
            var field = cls.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException caught) {
            throw new AssertionError(cls.getSimpleName() + " declares no " + name, caught);
        }
    }

    // ── Prefixes ───────────────────────────────────────────────────────

    @Test
    public void prefixesAreTwoLettersUniqueAndEveryNameStartsWithItsOwn() throws Exception {
        var keysClasses = keysClasses();
        Assert.assertTrue("keys classes not found under " + PRESENTATION_SOURCES.toAbsolutePath(),
                keysClasses.size() >= 4);

        var prefixes = new LinkedHashMap<Class<?>, String>();
        for (var cls : keysClasses) {
            var prefix = (String) staticField(cls, "PREFIX");
            Assert.assertTrue(cls.getSimpleName() + ": the prefix must be two lowercase letters, was '" + prefix + "'",
                    Pattern.matches("[a-z]{2}", prefix));
            prefixes.put(cls, prefix);
        }
        Assert.assertEquals("prefixes used by more than one keys class: " + prefixes,
                prefixes.size(), new HashSet<>(prefixes.values()).size());

        // every name a keys class declares, in its P and A tables, starts with its own prefix
        for (var cls : keysClasses) {
            for (var table : cls.getDeclaredClasses()) {
                for (var field : table.getDeclaredFields()) {
                    if (field.getType() == String.class && Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        var name = (String) field.get(null);
                        Assert.assertTrue(cls.getSimpleName() + ": '" + name + "' does not start with its prefix",
                                name.startsWith(prefixes.get(cls) + "-"));
                    }
                }
            }
        }
    }

    // ── Tree ───────────────────────────────────────────────────────────

    @Test
    public void eachPlaceIsDeclaredByOneKeysClassChildOfTheKeysOfItsParent() throws Exception {
        var placeOf = new HashMap<Class<?>, CubePlace>();
        for (var cls : keysClasses()) {
            Assert.assertTrue(cls.getSimpleName() + " does not extend CubeKeys", CubeKeys.class.isAssignableFrom(cls));
            placeOf.put(cls, (CubePlace) staticField(cls, "PLACE"));
        }

        Assert.assertEquals("a place declared by more than one keys class",
                placeOf.size(), new HashSet<>(placeOf.values()).size());
        Assert.assertEquals("every keys class is in ShoppingApplication.PLACES, which loads them",
                new HashSet<>(ShoppingApplication.PLACES), new HashSet<>(placeOf.values()));

        for (Map.Entry<Class<?>, CubePlace> e : placeOf.entrySet()) {
            var keys = e.getKey();
            var place = e.getValue();
            if (keys == RootKeys.class) {
                Assert.assertNull("the root keys declare the root", place.getParent());
            } else {
                Assert.assertSame(keys.getSimpleName() + ": its place must be a child of the place of the keys it extends",
                        placeOf.get(keys.getSuperclass()), place.getParent());
            }
            var instance = (CubeKeys<?>) keys.getConstructor(ShoppingApplication.class, CubeIntent.class)
                    .newInstance(this.app, new CubeIntent());
            Assert.assertSame(keys.getSimpleName() + ": its instances go to the place it declares", place,
                    instance.place());
        }
    }

    @Test
    public void anAddressResolvesToItsPlaceAndAnUnknownOneToTheRoot() {
        for (var place : ShoppingApplication.PLACES) {
            Assert.assertSame(place, this.app.resolvePlace(CubeIntent.parse(place.getName()).getPlace()));
        }
        Assert.assertSame(RootKeys.PLACE, this.app.resolvePlace(CubeIntent.parse("nowhere").getPlace()));
    }

    // ── Getters ────────────────────────────────────────────────────────

    private Long productIdOf(String query) {
        return new ProductKeys(this.app, CubeIntent.parse("product?" + query)).productId();
    }

    @Test
    public void gettersNeverTrustTheAddress() {
        Assert.assertEquals(Long.valueOf(7), productIdOf("pr-id=7"));
        Assert.assertNull(productIdOf("pr-id=abc"));
        Assert.assertNull(productIdOf("pr-id=-3"));
        Assert.assertEquals("ids start at 0 in this database", Long.valueOf(0), productIdOf("pr-id=0"));
        Assert.assertNull("the old name is no longer read", productIdOf("productId=7"));

        var receipt = new ReceiptKeys(this.app, CubeIntent.parse("receipt?rc-id=x"));
        Assert.assertNull(receipt.purchaseId());
        Assert.assertFalse("an attribute never comes from the address", receipt.purchaseMade());

        // the default leaves the address
        var keys = new ProductKeys(this.app, CubeIntent.parse("product?pr-id=7"));
        keys.withProductId(null);
        Assert.assertEquals("product", keys.intent().toString());
    }

    // ── Navigation ─────────────────────────────────────────────────────

    private void signIn() {
        new LoginKeys(this.app).flip();
        var loginView = LoginViewMock.cast(this.app.getRootView().state.contentView.get());
        loginView.state.onEnter.run("admin", "admin");
    }

    private HomeViewMock home() {
        return HomeViewMock.cast(this.app.getRootView().state.contentView.get());
    }

    @Test
    public void anAddressWithoutAUsableIdRedirectsToTheHome() {
        signIn();
        for (var address : List.of("product?pr-id=abc", "product", "receipt?rc-id=-1", "receipt", "public")) {
            this.app.go(address);
            Assert.assertNull(address + ": the home shows no inner place", home().state.contentView.get());
            Assert.assertSame(address, HomeKeys.PLACE, this.app.getLastPlace());
        }
    }

    @Test
    public void navigatingFromTheStatePutsTheNamesInTheAddress() {
        signIn();
        new ProductKeys(this.app).withProductId(DBReset.PEN_DRIVE2GB_ID).flip();
        var productView = ProductViewMock.cast(home().state.contentView.get());
        Assert.assertEquals((long) DBReset.PEN_DRIVE2GB_ID, (long) productView.state.product.get().id);
        Assert.assertTrue(this.app.newIntent().toString(),
                this.app.newIntent().toString().contains("pr-id=" + DBReset.PEN_DRIVE2GB_ID));

        this.app.go("receipt?rc-id=" + DBReset.ADMIN_SECOND_PURCHASE_ID);
        Assert.assertTrue(this.app.newIntent().toString(),
                this.app.newIntent().toString().contains("rc-id=" + DBReset.ADMIN_SECOND_PURCHASE_ID));
    }
}
