package com.manning.apisecurityinaction.token;

import org.json.JSONException;
import org.json.JSONObject;
import spark.Request;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;

public class JsonTokenStore implements TokenStore {
    @Override
    public String create(final Request request, final Token token) {
        final var json = new JSONObject();
        json.put("sub", token.getUsername());
        json.put("exp", token.getExpiry());
        json.put("attrs", token.getAttributes());

        final var jsonBytes = json.toString().getBytes(StandardCharsets.UTF_8);
        return Base64URL.encode(jsonBytes);
    }

    @Override
    public Optional<Token> read(final Request request, final String tokenId) {
        try {
            final var decoded = Base64URL.decode(tokenId);
            final var json = new JSONObject(new String(decoded, StandardCharsets.UTF_8));
            final var expiry = Instant.ofEpochSecond(json.getInt("exp"));
            final var username = json.getString("sub");
            final var attrs = json.getJSONObject("attrs");

            final var token = new Token(expiry, username);
            final var attributes = new HashMap<String, String>();
            for (final String key : attrs.keySet()) {
                attributes.put(key, attrs.getString("key"));
            }
            token.setAttributes(attributes);

            return Optional.of(token);
        } catch (JSONException e) {
            return Optional.empty();
        }
    }

    @Override
    public void revoke(final Request request, final String tokenId) {

    }
}
