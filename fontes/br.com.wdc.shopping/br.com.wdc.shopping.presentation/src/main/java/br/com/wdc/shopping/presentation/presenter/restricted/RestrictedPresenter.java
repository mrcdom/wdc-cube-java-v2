package br.com.wdc.shopping.presentation.presenter.restricted;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.Routes;

public class RestrictedPresenter implements CubePresenter {

	public final ShoppingApplication app;

	public RestrictedPresenter(ShoppingApplication app) {
		this.app = app;
	}

	@Override
	public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
		// :: Coloque regras necessárias para a parte restrita do site

		if (this.app.getSubject() == null) {
			// Se não temos usuário logado, não pode avançar na composição.
			// Então, direcionamos para a área aberta do site.
			Routes.open(this.app, intent);
			return false;
		}

		if (deepest) {
			// Se houve um direcionamento para este ponto, não podemos aceitar essa composição
			// pois é uma composição de guarda apenas. Não possui tela associada.
			// Então, direcionamentos para a tela default da área restrita
			Routes.home(this.app, intent);
			return false;
		}

		return true;
	}

	@Override
	public void publishParameters(CubeIntent intent) {
		// NOOP
	}

}
