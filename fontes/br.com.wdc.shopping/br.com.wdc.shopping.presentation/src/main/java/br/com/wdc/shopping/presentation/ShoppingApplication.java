package br.com.wdc.shopping.presentation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import br.com.wdc.framework.cube.CubeApplication;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeNavigation;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.security.SecurityContext;
import br.com.wdc.shopping.domain.security.SecurityContextHolder;
import br.com.wdc.shopping.presentation.function.GoAction;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartManager;

public abstract class ShoppingApplication extends CubeApplication {

    protected Subject subject;

    protected CartManager cart;

    private SecurityContext securityContext;

    private UserRepository userRepository;
    private ProductRepository productRepository;
    private PurchaseRepository purchaseRepository;
    private PurchaseItemRepository purchaseItemRepository;

    // :: Getters and Setters

    public CubePlace getRootPlace() {
        return Routes.Place.ROOT;
    }

    public RootPresenter getRootPresenter() {
        return (RootPresenter) this.presenterMap.get(Routes.Place.ROOT.getId());
    }

    public Subject getSubject() {
        return this.subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
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

    @SuppressWarnings("unchecked")
    @Override
    public CubeNavigation<ShoppingApplication> navigate() {
        return super.navigate();
    }

    // :: API

    public void alertUnexpectedError(Logger logger, String message, Throwable e) {
        var rootPresenter = this.getRootPresenter();
        if (rootPresenter != null) {
            rootPresenter.alertUnexpectedError(logger, message, e);
        }
    }

    public void go(String placeStr) throws Exception {
        this.go(CubeIntent.parse(placeStr));
    }

    public void go(CubeIntent intent) {
        ShoppingApplication.Internals.go(this, intent);
    }

    // :: Internal Classes - Meant to be user on initialization only

    public static class Internals {

        private static final Map<String, GoAction> goActionMap = new ConcurrentHashMap<>();

        private Internals() {
            super();
        }

        public static void registerPlace(String tag, GoAction goAction) {
            goActionMap.put(tag, goAction);
        }

        static Boolean go(ShoppingApplication app, CubeIntent place) {
            var goAction = goActionMap.get(place.getPlace().getName());
            if (goAction == null) {
                goAction = goActionMap.get(app.getRootPlace().getName());
            }

            if (goAction != null) {
                return goAction.apply(app, place);
            }
            return Boolean.FALSE;
        }

    }

    // :: Repository delegate factory

    /**
     * Cria um proxy que envolve cada chamada ao repositório com o SecurityContext
     * desta aplicação. Antes de delegar, seta o SecurityContextHolder na thread
     * corrente; ao final, restaura o estado anterior.
     */
    @SuppressWarnings("unchecked")
    private <T> T createDelegate(Class<T> repoInterface, T delegate) {
        if (delegate == null) {
            return null;
        }
        return (T) Proxy.newProxyInstance(
                repoInterface.getClassLoader(),
                new Class<?>[]{ repoInterface },
                new SecurityContextDelegate(this, delegate));
    }

    private static final class SecurityContextDelegate implements InvocationHandler {

        private final ShoppingApplication app;
        private final Object delegate;

        SecurityContextDelegate(ShoppingApplication app, Object delegate) {
            this.app = app;
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            var previous = SecurityContextHolder.get();
            try {
                var ctx = app.securityContext;
                if (ctx != null) {
                    SecurityContextHolder.set(ctx);
                }
                return method.invoke(delegate, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            } finally {
                if (previous != null) {
                    SecurityContextHolder.set(previous);
                } else {
                    SecurityContextHolder.clear();
                }
            }
        }
    }

}
