package br.com.wdc.framework.cube;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import br.com.wdc.framework.commons.log.Log;

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

	protected <T extends CubeApplication> CubeNavigation<T> navigate() {
        if (this.navigation != null) {
            this.navigation.interrupt();

            if (this.navigation.reflowCount > 10) {
                throw new AssertionError("Navigation recursion detected");
            }

            var newContext = new CubeNavigation<T>(this);
            newContext.reflowCount = this.navigation.reflowCount + 1;

            // Migrate created presenters from interrupted navigation
            newContext.newPresenterMap.putAll(this.navigation.newPresenterMap);

            this.navigation = newContext;
            return newContext;
        } else {
            var newContext = new CubeNavigation<T>(this);
            this.navigation = newContext;
            return newContext;
        }
	}

	// Abstract

	public abstract CubePlace getRootPlace();

	public abstract void updateHistory();
	
	protected abstract Map<Integer, CubePresenter> createPresenterMap();

	/**
	 * Factory for the attribute map. Override to return a thread-safe map
	 * in multi-threaded environments (e.g. ConcurrentHashMap).
	 */
	protected Map<String, Object> createAttributeMap() {
		return new java.util.HashMap<>();
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
