package br.com.wdc.framework.cube;

/**
 * The base of an application's keys classes: typed accessors over a {@link CubeIntent}, one class per place.
 * <ul>
 * <li><b>Each keys class declares its place</b>, in a static field, from the place of the keys it extends
 * ({@code PLACE = HomeKeys.PLACE.child("product", ProductPresenter::new)}). So the class tree is the place tree, and a
 * deeper presenter reads, through inherited accessors, what the levels above hand down. Every place has its keys, even
 * one with no names of its own: the keys declare it and navigate to it.</li>
 * <li><b>Built without an intent</b>, the keys start from the application's current state
 * ({@link CubeApplication#newIntent}, which asks every live presenter what it shows) and {@link #flip} goes to their
 * place: {@code new ProductKeys(app).withProductId(id).flip()}.</li>
 * <li><b>Built over the intent a presenter receives</b>, they read and write it in place. A redirect from inside
 * {@code applyParameters} uses the intent in transit — {@code new HomeKeys(app, intent).flip()} — not a new one: that
 * presenter is not accepted yet, and a new intent would lose what the address just brought.</li>
 * </ul>
 * A place declared in a static field only joins the tree when its class is loaded, so the application must reference
 * every keys class before resolving an address by name (a list of their places is enough).
 */
public abstract class CubeKeys<A extends CubeApplication> {

    protected final A app;

    private final CubeIntent intent;

    private final CubePlace place;

    /**
     * @param intent the intent to read and write; {@code null} for a new one, from the current state
     * @param place  the place of the keys class being built: where {@link #flip} goes
     */
    protected CubeKeys(A app, CubeIntent intent, CubePlace place) {
        this.app = app;
        this.place = place;
        if (intent != null) {
            this.intent = intent;
        } else {
            this.intent = app.newIntent();
            this.intent.setPlace(place);
        }
    }

    /** The intent these keys read and write. */
    public final CubeIntent intent() {
        return intent;
    }

    /** The place of these keys: where {@link #flip} goes. */
    public final CubePlace place() {
        return place;
    }

    /** Navigates to {@link #place()}, with what was set on {@link #intent()}. */
    public boolean flip() {
        return app.navigate(place, intent);
    }
}
