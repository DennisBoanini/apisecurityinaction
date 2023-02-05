package com.manning.apisecurityinaction.token;

import org.dalesbred.Database;
import org.json.JSONObject;
import spark.Request;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseTokenStore implements TokenStore {

    private final Database database;
    private final SecureRandom secureRandom;

    public DatabaseTokenStore(final Database database) {
        this.database = database;
        this.secureRandom = new SecureRandom();
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::deleteExpiredTokens, 10, 10, TimeUnit.MINUTES);
    }

    private String randomId() {
        final var bytes = new byte[20];
        secureRandom.nextBytes(bytes);

        return Base64URL.encode(bytes);
    }

    @Override
    public String create(Request request, TokenStore.Token token) {
        final var tokenId = randomId();
        final var attrs = new JSONObject(token.getAttributes()).toString();

        database.updateUnique(
                "INSERT INTO tokens(token_id, user_id, expiry, attributes) " +
                        "VALUES(?, ?, ?, ?)",
                hash(tokenId), token.getUsername(), token.getExpiry(), attrs
        );

        return tokenId;
    }

    @Override
    public Optional<Token> read(final Request request, final String tokenId) {
        return database.findOptional(this::readToken,
                "SELECT user_id, expiry, attributes " +
                        "FROM tokens " +
                        "WHERE token_id = ?",
                hash(tokenId)
        );
    }

    @Override
    public void revoke(final Request request, final String tokenId) {
        database.update("DELETE FROM tokens WHERE token_id = ?", hash(tokenId));
    }

    public void deleteExpiredTokens() {
        database.update("DELETE FROM tokens WHERE expiry < current_timestamp");
    }

    private Token readToken(ResultSet resultSet) throws SQLException {
        final var username = resultSet.getString(1);
        final var expiry = resultSet.getTimestamp(2).toInstant();
        final var json = new JSONObject(resultSet.getString(3));

        final var token = new Token(expiry, username);
        final Map<String, String> attrs = new HashMap<>();
        for (final String key : json.keySet()) {
            attrs.put(key, json.getString(key));
        }
        token.setAttributes(attrs);

        return token;
    }

    private String hash(String tokenId) {
        final var hash = CookieTokenStore.sha256(tokenId);

        return Base64URL.encode(hash);
    }
}
