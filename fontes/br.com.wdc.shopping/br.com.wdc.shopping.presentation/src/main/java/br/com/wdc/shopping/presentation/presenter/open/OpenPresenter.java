package br.com.wdc.shopping.presentation.presenter.open;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.RestrictedKeys;

public class OpenPresenter implements CubePresenter {
	
	public final ShoppingApplication app;
	
	public OpenPresenter(ShoppingApplication app) {
		this.app = app;
	}

	@Override
	public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        // :: Rules required by the public area of the site
	    
	    // An authenticated user has no reason to be in the public area
        if (this.app.getSubject() != null) {
            new RestrictedKeys(this.app, intent).flip();
            return false;
        }

        if (deepest) {
            new LoginKeys(this.app, intent).flip();
            return false;
        }

        return true;
	}

	@Override
	public void publishParameters(CubeIntent intent) {
		// NOOP
	}

}
