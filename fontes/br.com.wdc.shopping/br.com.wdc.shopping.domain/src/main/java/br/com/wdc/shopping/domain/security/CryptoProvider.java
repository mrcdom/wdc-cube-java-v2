package br.com.wdc.shopping.domain.security;

import java.util.concurrent.atomic.AtomicReference;

/**
 * SPI para operações criptográficas necessárias na autenticação.
 * <p>
 * Permite que implementações alternativas (ex: Web Crypto API no browser)
 * substituam a implementação JCE padrão.
 * <p>
 * Se nenhum provider for registrado, {@link PasswordUtil} usa a implementação
 * JCE do JVM (MessageDigest + javax.crypto.Mac).
 */
public interface CryptoProvider {

    AtomicReference<CryptoProvider> BEAN = new AtomicReference<>();

    /**
     * Hash MD5 de bytes, retornando os bytes do digest.
     */
    byte[] md5(byte[] input);

    /**
     * HMAC-SHA256.
     *
     * @param key  chave em bytes
     * @param data dados em bytes
     * @return bytes do HMAC
     */
    byte[] hmacSha256(byte[] key, byte[] data);

}
