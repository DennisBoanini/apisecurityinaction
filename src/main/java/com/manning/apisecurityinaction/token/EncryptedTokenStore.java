package com.manning.apisecurityinaction.token;

import software.pando.crypto.nacl.SecretBox;
import spark.Request;

import java.security.Key;
import java.util.Optional;

public class EncryptedTokenStore implements TokenStore {

    private final TokenStore delegate;
    private final Key encryptionKey;

    public EncryptedTokenStore(final TokenStore delegate, final Key encryptionKey) {
        this.delegate = delegate;
        this.encryptionKey = encryptionKey;
    }

    @Override
    public String create(final Request request, final Token token) {
        final var tokenId = delegate.create(request, token);

        return SecretBox.encrypt(encryptionKey, tokenId).toString();
    }

    @Override
    public Optional<Token> read(final Request request, final String tokenId) {
        final var box = SecretBox.fromString(tokenId);
        final var originalTokenId = box.decryptToString(this.encryptionKey);

        return this.delegate.read(request, originalTokenId);
    }

    @Override
    public void revoke(final Request request, final String tokenId) {
        final var box = SecretBox.fromString(tokenId);
        final var originalTokenId = box.decryptToString(this.encryptionKey);
        this.delegate.revoke(request, originalTokenId);
    }
}
