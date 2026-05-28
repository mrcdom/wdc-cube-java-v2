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

        if (deepest) {
            // Só temos uma tela pública, que é o Login

            if (this.app.getSubject() == null) {
                // ... logo, se não temos usuário logado, a tela default é justamente o login
                Routes.login(this.app, intent);
                return false;
            }

            // ..., contudo, se temos usuário logado, por não termos outras telas abertas,
            // a tela default é a principal da área restrita
            Routes.restricted(this.app, intent);
            return false;
        }

        return true;
	}

	@Override
	public void publishParameters(CubeIntent intent) {
		// NOOP
	}

}
