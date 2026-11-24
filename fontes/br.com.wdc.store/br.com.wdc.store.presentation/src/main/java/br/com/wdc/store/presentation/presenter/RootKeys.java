package br.com.wdc.store.presentation.presenter;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeKeys;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.store.presentation.StoreApplication;

/**
 * The names a place reads from and writes to the {@link CubeIntent}, typed, on the framework's {@link CubeKeys}.
 * <b>Each place has one keys class, which declares the place and is the only one that knows its names</b> — even a
 * place with no names of its own yet; presenters never read or write the intent with a literal name, and navigate
 * through the keys of the destination ({@code new CartKeys(app).flip()}).
 * <ul>
 * <li><b>A two-letter prefix per class, unique in the application</b> ({@link #PREFIX}); every name it declares starts
 * with it ({@code pr-id}). Names live in private tables: {@code P} for parameters (they go in the address), {@code A}
 * for attributes (they don't). To create a name, look only at your own file. {@code PlaceKeysTest} checks the
 * prefixes are unique.</li>
 * <li><b>The keys declare the place</b>, from the place of the keys they extend
 * ({@code PLACE = HomeKeys.PLACE.child("product", ProductPresenter::new)}), so the class tree is the place tree:
 * {@code RootKeys → RestrictedKeys → HomeKeys → ProductKeys}. A name is declared once, in the keys of whoever publishes
 * it; the places below inherit it to read. {@link StoreApplication#PLACES} lists them all, which also loads them;
 * {@code PlaceKeysTest} checks the tree.</li>
 * <li><b>The one name reused on purpose is the composition slot</b> ({@link #parentSlot()}): each level reads the slot
 * it got and republishes, under the same name, the slot of its content for the level below.</li>
 * <li><b>Validation is in the getter, which never throws</b>: the address is input from outside. A getter returns a
 * usable value or {@code null}. A setter given the default (or {@code null}) removes the name from the address.</li>
 * <li><b>Navigating starts from the state:</b> without an intent, the keys start from
 * {@link StoreApplication#newIntent}, which asks every live presenter what it shows —
 * {@code new ProductKeys(app).withProductId(id).flip()}.</li>
 * <li><b>Redirecting from inside {@code applyParameters} uses the intent in transit</b>
 * ({@code new HomeKeys(app, intent).flip()}), not a new one: that presenter is not accepted yet, and a new intent would
 * lose what the address just brought.</li>
 * </ul>
 * The root has no screen of its own: going to it goes on to the home (or to the login, when there is no user).
 */
public class RootKeys extends CubeKeys<StoreApplication> {

    public static final String PREFIX = "rt";

    public static final CubePlace PLACE = CubePlace.root("public", RootPresenter::new);

    private static final class P {
        /** The user signed in; published by the root, never accepted from the address. */
        static final String USER_ID = PREFIX + "-user";
    }

    private static final class A {
        /** Where the level below shows its view; each level republishes it for the next. */
        static final String PARENT_SLOT = PREFIX + "-parent-slot";
    }

    public RootKeys(StoreApplication app) {
        this(app, null);
    }

    public RootKeys(StoreApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }

    protected RootKeys(StoreApplication app, CubeIntent intent, CubePlace place) {
        super(app, intent, place);
    }

    /** The slot where this level shows its scope; a level with content republishes it for the level below. */
    public ScopeSlot parentSlot() {
        return intent().getSlot(A.PARENT_SLOT);
    }

    public RootKeys withParentSlot(ScopeSlot slot) {
        intent().setSlot(A.PARENT_SLOT, slot);
        return this;
    }

    /** An id (0 or more), or {@code null}. */
    public Long userId() {
        var id = intent().getParameterAsLong(P.USER_ID, null);
        return id != null && id >= 0 ? id : null;
    }

    public RootKeys withUserId(Long id) {
        intent().setParameter(P.USER_ID, id != null && id >= 0 ? id : null);
        return this;
    }
}
