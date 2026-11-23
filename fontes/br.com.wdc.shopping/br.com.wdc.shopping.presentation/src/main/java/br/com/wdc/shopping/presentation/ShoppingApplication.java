package br.com.wdc.shopping.presentation;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.CubeApplication;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.shopping.domain.ShoppingTransactions;
import br.com.wdc.shopping.domain.product.ProductRepository;
import br.com.wdc.shopping.domain.purchase.PurchaseRepository;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.shopping.domain.user.UserRepository;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartManager;

public abstract class ShoppingApplication extends CubeApplication {

	private static final Log LOG = Log.getLogger(ShoppingApplication.class);

	protected Subject subject;

	protected CartManager cart;

	private SecurityContext securityContext;

	private UserRepository userRepository;
	private ProductRepository productRepository;
	private PurchaseRepository purchaseRepository;
	private PurchaseItemRepository purchaseItemRepository;

	// :: Getters and Setters

	@Override
	public CubePlace getRootPlace() {
		return Routes.ROOT;
	}

	public RootPresenter getRootPresenter() {
		return (RootPresenter) this.presenterMap.get(Routes.ROOT.getId());
	}

	public Subject getSubject() {
		return this.subject;
	}

	public void setSubject(Subject subject) {
		this.subject = subject;
	}
	
	public TransactionService tx() {
	    return ShoppingTransactions.BEAN.get();
	}

	public SecurityContext getSecurityContext() {
		return this.securityContext;
	}

	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
		// Recriar delegates quando o contexto muda
		this.userRepository = null;
		this.productRepository = null;
		this.purchaseRepository = null;
		this.purchaseItemRepository = null;
	}

	public UserRepository getUserRepository() {
		if (userRepository == null) {
			userRepository = createDelegate(UserRepository.class, UserRepository.BEAN.get());
		}
		return userRepository;
	}

	public ProductRepository getProductRepository() {
		if (productRepository == null) {
			productRepository = createDelegate(ProductRepository.class, ProductRepository.BEAN.get());
		}
		return productRepository;
	}

	public PurchaseRepository getPurchaseRepository() {
		if (purchaseRepository == null) {
			purchaseRepository = createDelegate(PurchaseRepository.class, PurchaseRepository.BEAN.get());
		}
		return purchaseRepository;
	}

	public PurchaseItemRepository getPurchaseItemRepository() {
		if (purchaseItemRepository == null) {
			purchaseItemRepository = createDelegate(PurchaseItemRepository.class, PurchaseItemRepository.BEAN.get());
		}
		return purchaseItemRepository;
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

	// :: Repository delegate factory

	/**
	 * Cria (opcionalmente) um wrapper para o repositório que pode envolver cada chamada com o SecurityContext desta aplicação.
	 * <p>
	 * Implementação padrão retorna o delegate diretamente (sem proxy). Subclasses em ambientes multi-threaded (JVM desktop/server) devem override para usar
	 * {@link ProxyRepositoryWrapper#wrap(Class, Object, java.util.function.Supplier)}.
	 */
	@SuppressWarnings("java:S1172") // repoInterface is used by subclass overrides (ProxyRepositoryWrapper)
	protected <T> T createDelegate(Class<T> repoInterface, T delegate) {
		return delegate;
	}

}
