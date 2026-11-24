package br.com.wdc.framework.cube;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.storage.ClientStorage;

public abstract class CubeApplication {
    
    private static final Log LOG = Log.getLogger(CubeApplication.class.getSimpleName());

	protected Map<Integer, CubePresenter> presenterMap;

	protected Map<String, Object> attributeMap;

	protected CubePlace lastPlace;

	protected String fragment;

	protected CubeApplication() {
		this.presenterMap = this.createPresenterMap();
		this.attributeMap = this.createAttributeMap();
	}

	public void release() {
		var presenterIds = new ArrayList<Integer>();
		presenterIds.addAll(presenterMap.keySet());

		presenterIds.sort(Comparator.reverseOrder());

		for (var presenterId : presenterIds) {
			var presenter = presenterMap.remove(presenterId);
			if (presenter != null) {
				try {
					presenter.release();
				} catch (Exception caught) {
				    LOG.error("releasing " + presenter.getClass() + ": " + caught.getMessage());
				}
			}
		}

		this.presenterMap.clear();
	}

	public CubePlace getLastPlace() {
		return this.lastPlace;
	}

	public String getFragment() {
		return this.fragment;
	}

	public void setFragment(String fragment) {
		this.fragment = fragment;
	}

	public void publishParameters(CubeIntent intent) {
		for (var presenter : this.presenterMap.values()) {
			presenter.publishParameters(intent);
		}
	}

	public void commitComputedState() {
		for (var presenter : this.presenterMap.values()) {
			try {
				presenter.commitComputedState();
			} catch (Exception caught) {
			    LOG.error("Processing " + presenter.getClass().getSimpleName() + ": " + caught.getMessage());
			}
		}
	}

	public CubeIntent newIntent() {
		var place = new CubeIntent();
		place.setPlace(this.lastPlace);
		this.publishParameters(place);
		return place;
	}

	CubeNavigation<?> navigation;

	/** Goes to the place, walking its path from the root. {@code false} when some presenter interrupted the walk. */
	public boolean navigate(CubePlace target, CubeIntent intent) {
		return this.newNavigation().execute(target, intent);
	}

	/** Goes to the place named in the address ({@code name?param=value}). */
	public boolean go(String address) {
		return this.go(CubeIntent.parse(address));
	}

	/** Goes to the intent's place, resolving it by name in the place tree; an unknown name goes to the root. */
	public boolean go(CubeIntent intent) {
		var place = this.resolvePlace(intent.getPlace());
		intent.setPlace(place);
		return this.navigate(place, intent);
	}

	/** The place of the tree with the same name; the root when there is none. */
	public CubePlace resolvePlace(CubePlace place) {
		var root = this.getRootPlace();
		if (place == null) {
			return root;
		}
		if (!place.isUnresolved()) {
			return place;
		}
		var found = root.find(place.getName());
		return found != null ? found : root;
	}

	private CubeNavigation<?> newNavigation() {
        if (this.navigation != null) {
            this.navigation.interrupt();

            if (this.navigation.reflowCount > 10) {
                throw new AssertionError("Navigation recursion detected");
            }

            var newContext = new CubeNavigation<>(this);
            newContext.reflowCount = this.navigation.reflowCount + 1;

            // Migrate created presenters from interrupted navigation
            newContext.newPresenterMap.putAll(this.navigation.newPresenterMap);

            this.navigation = newContext;
            return newContext;
        } else {
            var newContext = new CubeNavigation<>(this);
            this.navigation = newContext;
            return newContext;
        }
	}

	// Abstract

	public abstract CubePlace getRootPlace();

	public abstract void updateHistory();

	/** Session storage — in-memory, lives while the app instance is active. */
	public abstract ClientStorage clientSessionStore();

	/** Persistent storage — survives restarts; call {@code .secure()} for secure backing. */
	public abstract ClientStorage clientPersistentStore();
	
	protected abstract Map<Integer, CubePresenter> createPresenterMap();

	/**
	 * Factory for the attribute map. Override to return a thread-safe map
	 * in multi-threaded environments (e.g. ConcurrentHashMap).
	 */
	protected Map<String, Object> createAttributeMap() {
		return new java.util.HashMap<>();
	}

	/** A failure nobody handled, such as one caught by a guarded action. Logs it; the application may also show it. */
	public void alertUnexpectedError(String message, Throwable error) {
		LOG.error(message, error);
	}

	// :: Views of scopes

	private ScopeViews scopeViews = new ScopeViews();

	private final List<Scope> liveScopes = new ArrayList<>();

	/** The view layer's catalog of views by scope class. */
	public void setScopeViews(ScopeViews scopeViews) {
		this.scopeViews = scopeViews;
	}

	/**
	 * The view that draws the scope, made the first time it is asked for: from then on, a change in the scope redraws
	 * only that view. A scope that already has one (a client rebuilding scopes from a remote host attached it) keeps
	 * it. {@code null} for no scope.
	 */
	public CubeView viewOf(Scope scope) {
		if (scope == null) {
			return null;
		}
		var existing = scope.view();
		if (existing != null) {
			return existing;
		}
		// Made outside the lock: a factory may hop to the UI thread, which may be asking for another view meanwhile
		var made = this.scopeViews.factoryFor(scope).apply(this, scope);
		synchronized (this.liveScopes) {
			existing = scope.view();
			if (existing == null) {
				scope.attachView(made);
				scope.setUpdater(made::update);
				this.liveScopes.add(scope);
			}
		}
		if (existing != null) {
			// Someone else made it first
			made.release();
			return existing;
		}
		// The first drawing: the view may need it before any change (to measure itself, for instance)
		made.update();
		return made;
	}

	/** The views made so far and not yet released: the presenters', the segments' and the rows'. */
	public List<CubeView> liveViews() {
		synchronized (this.liveScopes) {
			var views = new ArrayList<CubeView>(this.liveScopes.size());
			for (var live : this.liveScopes) {
				var view = live.view();
				if (view != null) {
					views.add(view);
				}
			}
			return views;
		}
	}

	/** Releases the view of the scope and of every scope inside it (its segments, its rows). */
	public void releaseView(Scope scope) {
		if (scope == null) {
			return;
		}
		var released = new ArrayList<Scope>();
		synchronized (this.liveScopes) {
			var it = this.liveScopes.iterator();
			while (it.hasNext()) {
				var live = it.next();
				if (isWithin(live, scope)) {
					it.remove();
					released.add(live);
				}
			}
		}
		for (var live : released) {
			var view = live.view();
			live.setUpdater(null);
			live.attachView(null);
			if (view != null) {
				try {
					view.release();
				} catch (Exception caught) {
					LOG.error("Releasing the view of " + live.getClass().getSimpleName() + ": " + caught.getMessage());
				}
			}
		}
	}

	private static boolean isWithin(Scope scope, Scope ancestor) {
		for (var s = scope; s != null; s = s.owner()) {
			if (s == ancestor) {
				return true;
			}
		}
		return false;
	}

	// :: Attributes

	public Object setAttribute(String name, Object value) {
		return this.attributeMap.put(name, value);
	}

	public Object getAttribute(String name) {
		return this.attributeMap.get(name);
	}

	public Object removeAttribute(String name) {
		return this.attributeMap.remove(name);
	}

}
