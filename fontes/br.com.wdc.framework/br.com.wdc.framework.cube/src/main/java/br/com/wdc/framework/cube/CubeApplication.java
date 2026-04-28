package br.com.wdc.framework.cube;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CubeApplication {

	static final Logger LOG = LoggerFactory.getLogger(CubeApplication.class);

	protected ConcurrentHashMap<Integer, CubePresenter> presenterMap;

	protected CubePlace lastPlace;

	protected String fragment;

	protected CubeApplication() {
		this.presenterMap = new ConcurrentHashMap<>();
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
					LOG.error("releasing " + presenter.getClass(), caught);
				}
			}
		}

		this.presenterMap.clear();
	}

	public String getFragment() {
		return this.fragment;
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
				LOG.error("Processing " + presenter.getClass().getSimpleName(), caught);
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
			newContext = new CubeNavigation<T>(this);
			this.navigation = newContext;
			return newContext;
		} else {
			var newContext = new CubeNavigation<T>(this);
			this.navigation = newContext;
			return newContext;
		}
	}

	// Abstract

	public abstract Object setAttribute(String name, Object value);

	public abstract Object getAttribute(String name);

	public abstract Object removeAttribute(String name);

	public abstract void updateHistory();

}
