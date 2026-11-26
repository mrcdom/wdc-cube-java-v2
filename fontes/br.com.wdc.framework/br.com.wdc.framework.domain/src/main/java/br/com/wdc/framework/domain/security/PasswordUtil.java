package br.com.wdc.framework.domain.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * The password digest the sign-in starts from: MD5 → signed {@code BigInteger} → base 36. It is what {@link Scram}
 * derives the keys from, and the form passwords were stored in before the SCRAM verifiers, so stored digests convert
 * to verifiers without the passwords ({@link PasswordVerifiers#fromDigest}).
 */
public final class PasswordUtil {

	private PasswordUtil() {
	}

	/** The digest of {@code plainPassword} on the provider set for the process ({@link CryptoProvider#BEAN}). */
	public static String hashPassword(String plainPassword) {
		var provider = CryptoProvider.BEAN.get();
		if (provider == null) {
			throw new IllegalStateException("CryptoProvider.BEAN not initialized");
		}
		return hashPassword(plainPassword, provider);
	}

	/** The digest of {@code plainPassword} on {@code crypto}. */
	public static String hashPassword(String plainPassword, CryptoProvider crypto) {
		byte[] digest = crypto.md5(plainPassword.getBytes(StandardCharsets.UTF_8));
		return new BigInteger(digest).toString(36);
	}

}
