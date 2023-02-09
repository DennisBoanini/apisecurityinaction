package com.manning.apisecurityinaction.controller;

import com.manning.apisecurityinaction.token.SecureTokenStore;
import com.manning.apisecurityinaction.token.TokenStore;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.time.temporal.ChronoUnit;

import static java.time.Instant.now;
import static spark.Spark.halt;

public class TokenController {

    private final TokenStore tokenStore;

    public TokenController(final SecureTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public JSONObject login(Request request, Response response) {
        final String subject = request.attribute("subject");
        final var expiry = now().plus(10, ChronoUnit.MINUTES);
        final var token = new TokenStore.Token(expiry, subject);
        final var tokenId = this.tokenStore.create(request, token);

        response.status(201);
        return new JSONObject()
                .put("token", tokenId);
    }

    public void validateToken(Request request, Response response) {
        var tokenId = request.headers("Authorization");
        if (tokenId == null || !tokenId.startsWith("Bearer ")) {
            return;
        }
        tokenId = tokenId.substring(7);

        tokenStore.read(request, tokenId).ifPresent(token -> {
            if (now().isBefore(token.getExpiry())) {
                request.attribute("subject", token.getUsername());
                token.getAttributes().forEach(request::attribute);
            } else {
                response.header("WWW-Authentication", "Bearer error=\"invalid_token\", error_description=\"Expired\"");
                halt(401);
            }
        });
    }

    public JSONObject logout(Request request, Response response) {
        var tokenId = request.headers("Authorization");
        if (tokenId == null || !tokenId.startsWith("Bearer ")) {
            throw new IllegalArgumentException("missing token header");
        }
        tokenId = tokenId.substring(7);

        tokenStore.revoke(request, tokenId);
        response.status(200);

        return new JSONObject();
    }
}
