package br.com.wdc.framework.cube;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * The view layer's catalog: which view draws each scope class. Each host (Vaadin, SWT, TeaVM, the remote host) builds
 * its own and hands it to the application ({@link CubeApplication#setScopeViews}); presenters never see it — they
 * only make scopes. A scope class with no view of its own is looked up by its superclasses, then by the fallback.
 */
public final class ScopeViews {

    private final Map<Class<?>, BiFunction<CubeApplication, Scope, CubeView>> factories = new ConcurrentHashMap<>();

    private volatile BiFunction<CubeApplication, Scope, CubeView> fallback;

    /** Pairs a scope class with the view that draws it. */
    @SuppressWarnings("unchecked")
    public <A extends CubeApplication, S extends Scope> ScopeViews register(Class<S> scopeClass,
            BiFunction<A, S, CubeView> factory) {
        factories.put(scopeClass, (BiFunction<CubeApplication, Scope, CubeView>) (BiFunction<?, ?, ?>) factory);
        return this;
    }

    /** The view for a scope class nobody registered (the remote host draws every scope the same way). */
    @SuppressWarnings("unchecked")
    public <A extends CubeApplication> ScopeViews fallback(BiFunction<A, Scope, CubeView> factory) {
        this.fallback = (BiFunction<CubeApplication, Scope, CubeView>) (BiFunction<?, ?, ?>) factory;
        return this;
    }

    BiFunction<CubeApplication, Scope, CubeView> factoryFor(Scope scope) {
        for (Class<?> c = scope.getClass(); c != null && c != Scope.class; c = c.getSuperclass()) {
            var factory = factories.get(c);
            if (factory != null) {
                return factory;
            }
        }
        if (fallback != null) {
            return fallback;
        }
        throw new IllegalStateException("No view registered for scope " + scope.getClass().getName());
    }
}
