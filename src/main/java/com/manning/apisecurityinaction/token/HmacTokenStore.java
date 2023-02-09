package com.manning.apisecurityinaction.token;

import javax.crypto.Mac;
import spark.Request;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Optional;

public class HmacTokenStore implements SecureTokenStore {

    private final TokenStore delegate;
    private final Key macKey;

    private HmacTokenStore(final TokenStore delegate, final Key macKey) {
        this.delegate = delegate;
        this.macKey = macKey;
    }

    public static SecureTokenStore wrap(ConfidentialTokenStore store, Key macKey) {
        return new HmacTokenStore(store, macKey);
    }

    public static SecureTokenStore wrap(TokenStore store, Key macKey) {
        return new HmacTokenStore(store, macKey);
    }

    @Override
    public String create(final Request request, final Token token) {
        final var tokenId = delegate.create(request, token);
        final var tag = this.hmac(tokenId);

        return tokenId + "." + Base64URL.encode(tag);
    }

    @Override
    public Optional<Token> read(final Request request, final String tokenId) {
        final var index = tokenId.lastIndexOf(".");
        if (index == -1) {
            return Optional.empty();
        }

        final var realTokenId = tokenId.substring(0, index);
        final var provided = Base64URL.decode(tokenId.substring(index + 1));
        final var computed = this.hmac(realTokenId);
        if (!MessageDigest.isEqual(provided, computed)) {
            return Optional.empty();
        }

        return delegate.read(request, realTokenId);
    }

    @Override
    public void revoke(final Request request, final String tokenId) {

    }

    private byte[] hmac(String tokenId) {
        try {
            final var mac = Mac.getInstance(macKey.getAlgorithm());
            mac.init(macKey);

            return mac.doFinal(tokenId.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
