package br.com.wdc.shopping.presentation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import br.com.wdc.shopping.domain.security.SecurityContext;
import br.com.wdc.shopping.domain.security.SecurityContextHolder;

/**
 * Cria proxies dinâmicos para repositórios que envolvem cada chamada com o SecurityContext.
 * <p>
 * Uso típico em ambientes JVM multi-threaded (desktop, servidor).
 * NÃO deve ser usado em ambientes que não suportam reflection (TeaVM, GraalVM native sem config).
 * <p>
 * Para usar, override {@link ShoppingApplication#createDelegate(Class, Object)} na subclasse:
 * <pre>
 * protected &lt;T&gt; T createDelegate(Class&lt;T&gt; repoInterface, T delegate) {
 *     return ProxyRepositoryWrapper.wrap(repoInterface, delegate, this::getSecurityContext);
 * }
 * </pre>
 */
public final class ProxyRepositoryWrapper {

    private ProxyRepositoryWrapper() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T wrap(Class<T> repoInterface, T delegate, java.util.function.Supplier<SecurityContext> contextSupplier) {
        if (delegate == null) {
            return null;
        }
        return (T) Proxy.newProxyInstance(
                repoInterface.getClassLoader(),
                new Class<?>[]{ repoInterface },
                new SecurityContextDelegate(delegate, contextSupplier));
    }

    private static final class SecurityContextDelegate implements InvocationHandler {

        private final Object delegate;
        private final java.util.function.Supplier<SecurityContext> contextSupplier;

        SecurityContextDelegate(Object delegate, java.util.function.Supplier<SecurityContext> contextSupplier) {
            this.delegate = delegate;
            this.contextSupplier = contextSupplier;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            var previous = SecurityContextHolder.get();
            try {
                var ctx = contextSupplier.get();
                if (ctx != null) {
                    SecurityContextHolder.set(ctx);
                }
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
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
