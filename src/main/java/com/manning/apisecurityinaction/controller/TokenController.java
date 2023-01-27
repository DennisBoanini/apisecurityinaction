package com.manning.apisecurityinaction.controller;

import com.manning.apisecurityinaction.token.TokenStore;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.time.temporal.ChronoUnit;

import static java.time.Instant.now;

public class TokenController {

    private final TokenStore tokenStore;

    public TokenController(final TokenStore tokenStore) {
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
}