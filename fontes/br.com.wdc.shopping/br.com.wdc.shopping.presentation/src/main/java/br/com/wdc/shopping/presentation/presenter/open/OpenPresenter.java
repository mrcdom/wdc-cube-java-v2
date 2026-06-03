package br.com.wdc.shopping.presentation.presenter.open;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.Routes;

public class OpenPresenter implements CubePresenter {
	
	public final ShoppingApplication app;
	
	public OpenPresenter(ShoppingApplication app) {
		this.app = app;
	}

	@Override
	public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        // :: Coloque regras necessárias para a parte pública do site
	    
	    // Se o usuário já está autenticado, não há motivo para estar na área pública
        if (this.app.getSubject() != null) {
            Routes.restricted(this.app, intent);
            return false;
        }

        if (deepest) {
            Routes.login(this.app, intent);
            return false;
        }

        return true;
	}

	@Override
	public void publishParameters(CubeIntent intent) {
		// NOOP
	}

}
