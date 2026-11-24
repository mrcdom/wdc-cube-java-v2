package br.com.wdc.store.presentation.presenter.restricted;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.open.OpenKeys;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeKeys;

public class RestrictedPresenter implements CubePresenter {

	public final StoreApplication app;

	public RestrictedPresenter(StoreApplication app) {
		this.app = app;
	}

	@Override
	public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
		// :: Rules required by the restricted area of the site

		if (this.app.getSubject() == null) {
			// With no signed-in user, the composition cannot go further.
			// So we redirect to the open area of the site.
			new OpenKeys(this.app, intent).flip();
			return false;
		}

		if (deepest) {
			// A navigation that ends at this point cannot be accepted, since this
			// composition is only a guard and has no screen of its own.
			// So we redirect to the default screen of the restricted area
			new HomeKeys(this.app, intent).flip();
			return false;
		}

		return true;
	}

	@Override
	public void publishParameters(CubeIntent intent) {
		// NOOP
	}

}
