package br.com.wdc.framework.cube;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A node of an application's place tree. The path from the root down to a place is the chain of presenters that
 * navigation walks; the place name, unique across the tree, is what goes in the address.
 * <p>
 * The tree is immutable and is meant to be built as constants, so every application instance shares it:
 *
 * <pre>
 * public static final CubePlace ROOT = CubePlace.root("public", RootPresenter::new);
 * public static final CubePlace HOME = ROOT.child("home", HomePresenter::new);
 * </pre>
 *
 * The id grows with the depth ({@code level * 1000 + order}), so releasing presenters in reverse id order releases the
 * deepest first.
 */
public final class CubePlace {

    private static final Map<Integer, Integer> LAST_ID_BY_LEVEL = new HashMap<>();

    /** Creates the root of a place tree. */
    public static <A extends CubeApplication> CubePlace root(String name,
            Function<A, ? extends CubePresenter> factory) {
        return new CubePlace(name, null, factory);
    }

    /** A place outside any tree, known only by name: what {@link CubeIntent#parse} yields before resolution. */
    static CubePlace unresolved(String name) {
        return new CubePlace(name);
    }

    private final int id;

    private final String name;

    private final CubePlace parent;

    private final Function<? extends CubeApplication, ? extends CubePresenter> factory;

    private final List<CubePlace> path;

    private final List<CubePlace> children = new ArrayList<>();

    private final Map<String, CubePlace> byName;

    private CubePlace(String name) {
        this.id = -1;
        this.name = name;
        this.parent = null;
        this.factory = null;
        this.path = List.of();
        this.byName = Map.of();
    }

    private CubePlace(String name, CubePlace parent, Function<? extends CubeApplication, ? extends CubePresenter> factory) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("A place needs a name");
        }
        if (factory == null) {
            throw new IllegalArgumentException("The place " + name + " needs a presenter factory");
        }

        var p = new ArrayList<CubePlace>();
        if (parent != null) {
            p.addAll(parent.path);
        }
        p.add(this);

        this.name = name;
        this.parent = parent;
        this.factory = factory;
        this.path = Collections.unmodifiableList(p);
        this.id = nextId(p.size() - 1);

        if (parent == null) {
            this.byName = new HashMap<>();
        } else {
            this.byName = parent.byName;
            parent.children.add(this);
        }

        if (this.byName.putIfAbsent(name, this) != null) {
            throw new IllegalArgumentException("There is already a place named " + name + " in this tree");
        }
    }

    private static synchronized int nextId(int level) {
        var last = LAST_ID_BY_LEVEL.get(level);
        var next = last == null ? level * 1000 : last + 1;
        LAST_ID_BY_LEVEL.put(level, next);
        return next;
    }

    /** Creates a place below this one. */
    public <A extends CubeApplication> CubePlace child(String name, Function<A, ? extends CubePresenter> factory) {
        if (this.id == -1) {
            throw new IllegalStateException("The unresolved place " + this.name + " cannot have children");
        }
        return new CubePlace(name, this, factory);
    }

    /** Unique per place and growing with the depth; {@code -1} for an unresolved place. */
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CubePlace getParent() {
        return parent;
    }

    /** The places from the root down to this one, both included. Empty for an unresolved place. */
    public List<CubePlace> getPath() {
        return path;
    }

    public List<CubePlace> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /** A place of the same tree, by name; {@code null} when there is none. */
    public CubePlace find(String placeName) {
        return byName.get(placeName);
    }

    /** Whether this is a place known only by name, outside any tree. */
    public boolean isUnresolved() {
        return id == -1;
    }

    @SuppressWarnings("unchecked")
    <A extends CubeApplication> CubePresenter createPresenter(A app) {
        if (factory == null) {
            throw new IllegalStateException("The unresolved place " + name + " has no presenter");
        }
        return ((Function<A, ? extends CubePresenter>) factory).apply(app);
    }

    @Override
    public String toString() {
        return name;
    }
}
