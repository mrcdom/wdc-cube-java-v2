package br.com.wdc.store.presentation;

@SuppressWarnings("java:S1214")
public interface StorageKeys {

    String AUTH_TOKEN = "auth.token";
    String AUTH_ACCESS_TOKEN = "auth.accessToken";
    String AUTH_REFRESH_TOKEN = "auth.refreshToken";

    /** The cart, in the session scope: it survives a page reload, not the end of the session. */
    String CART = "cart";

}
