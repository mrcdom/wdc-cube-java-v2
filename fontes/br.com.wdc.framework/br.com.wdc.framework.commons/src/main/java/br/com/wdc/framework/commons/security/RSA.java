package br.com.wdc.framework.commons.security;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Random;

public class RSA implements Serializable {

    private static final long serialVersionUID = 6984014666530371360L;

    public static final BigInteger N65537 = BigInteger.valueOf(65537);

    private BigInteger privateKey;
    private BigInteger publicExponent;
    private BigInteger publicKey;

    // generate an N-bit (roughly) public and private key
    public RSA(int n, Random random) {
        var p = BigInteger.probablePrime(n / 2, random);
        var q = BigInteger.probablePrime(n / 2, random);
        var phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));

        publicKey = p.multiply(q);
        publicExponent = N65537; // common value in practice = 2^16
                                 // + 1
        privateKey = publicExponent.modInverse(phi);
    }

    public RSA(BigInteger publicExponent, BigInteger privateKey, BigInteger publicKey) {
        this.publicExponent = publicExponent;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public BigInteger getPrivateKey() {
        return this.privateKey;
    }

    public BigInteger getPublicExponent() {
        return publicExponent;
    }

    public BigInteger getPublicKey() {
        return publicKey;
    }

    public BigInteger encrypt(BigInteger message) {
        return message.modPow(publicExponent, publicKey);
    }

    public BigInteger decrypt(BigInteger encrypted) {
        return encrypted.modPow(privateKey, publicKey);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder(256)
                .append("publicExponent  = ").append(publicExponent.toString(16)).append('\n')
                .append("publicKey  = ").append(publicKey.toString(16)).append('\n')
                .append("privateKey  = ").append(privateKey.toString(16)).append('\n');
        return sb.toString();
    }

}
