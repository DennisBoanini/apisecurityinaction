package com.manning.apisecurityinaction.token;

import spark.Request;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class CookieTokenStore implements TokenStore {
    @Override
    public String create(final Request request, final Token token) {
        var session = request.session(false);
        if (session != null) {
            session.invalidate();
        }


        session = request.session(true);
        session.attribute("username", token.getUsername());
        session.attribute("expiry", token.getExpiry());
        session.attribute("attrs", token.getAttributes());

        return Base64URL.encode(sha256(session.id()));
    }

    @Override
    public Optional<Token> read(final Request request, final String tokenId) {
        final var session = request.session(false);
        if (session == null) {
            return Optional.empty();
        }

        var provided = Base64URL.decode(tokenId);
        var computed = sha256(session.id());
        if (!MessageDigest.isEqual(computed, provided)) {
            return Optional.empty();
        }

        final var token = new Token(session.attribute("expiry"), session.attribute("username"));
        token.setAttributes(session.attribute("attrs"));

        return Optional.of(token);
    }

    static byte[] sha256(String tokenId) {
        try {
            var sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(tokenId.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae);
        }
    }
}
