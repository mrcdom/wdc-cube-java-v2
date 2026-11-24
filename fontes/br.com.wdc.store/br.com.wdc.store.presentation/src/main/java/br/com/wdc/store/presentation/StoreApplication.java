package br.com.wdc.store.presentation;

import java.util.List;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.CubeApplication;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.store.presentation.presenter.RootKeys;
import br.com.wdc.store.presentation.presenter.RootPresenter;
import br.com.wdc.store.presentation.presenter.open.OpenKeys;
import br.com.wdc.store.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.store.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.store.presentation.presenter.restricted.RestrictedKeys;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartKeys;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartManager;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.store.presentation.presenter.restricted.products.ProductKeys;
import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptKeys;

public abstract class StoreApplication extends CubeApplication {

	private static final Log LOG = Log.getLogger(StoreApplication.class);

	/**
	 * Every place of the application, declared by its keys class. Listing them loads those classes, so the tree is
	 * whole before an address is resolved by name ({@code PlaceKeysTest} checks no keys class is missing).
	 */
	public static final List<CubePlace> PLACES = List.of(
			RootKeys.PLACE,
			OpenKeys.PLACE,
			LoginKeys.PLACE,
			RestrictedKeys.PLACE,
			HomeKeys.PLACE,
			CartKeys.PLACE,
			ProductKeys.PLACE,
			ReceiptKeys.PLACE);

	protected Subject subject;

	protected CartManager cart;

	// :: Getters and Setters

	@Override
	public CubePlace getRootPlace() {
		return RootKeys.PLACE;
	}

	public RootPresenter getRootPresenter() {
		return (RootPresenter) this.presenterMap.get(RootKeys.PLACE.getId());
	}

	public Subject getSubject() {
		return this.subject;
	}

	public void setSubject(Subject subject) {
		this.subject = subject;
	}

	public CartManager getCart() {
		return this.cart;
	}

	public CartManager setCart(CartManager cart) {
		var old = this.cart;
		this.cart = cart;
		return old;
	}

	// :: Extentions

	public abstract String b64Cipher(String text);

	public abstract String b64Decipher(String b64Text);
	
	public abstract String getClientIp();

	@Override
	public void alertUnexpectedError(String message, Throwable error) {
		this.alertUnexpectedError(LOG, message, error);
	}

	public void alertUnexpectedError(Log logger, String message, Throwable e) {
		var rootPresenter = this.getRootPresenter();
		if (rootPresenter != null) {
			rootPresenter.alertUnexpectedError(logger, message, e);
		}
	}

}
