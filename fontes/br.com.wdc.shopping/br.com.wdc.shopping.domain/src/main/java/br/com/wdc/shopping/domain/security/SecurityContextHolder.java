package br.com.wdc.shopping.domain.security;

/**
 * Holder ThreadLocal para o {@link SecurityContext} da requisição corrente.
 * <p>
 * Deve ser populado no início da requisição (ex: filtro HTTP) e limpo ao final.
 */
public final class SecurityContextHolder {

	private static final ThreadLocal<SecurityContext> HOLDER = new ThreadLocal<>();

	private SecurityContextHolder() {
	}

	public static SecurityContext get() {
		return HOLDER.get();
	}

	public static void set(SecurityContext ctx) {
		HOLDER.set(ctx);
	}

	public static void clear() {
		HOLDER.remove();
	}

}
