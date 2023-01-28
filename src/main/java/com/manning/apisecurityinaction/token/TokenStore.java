package com.manning.apisecurityinaction.token;

import spark.Request;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public interface TokenStore {

    String create(Request request, Token token);
    Optional<Token> read(Request request, String tokenId);

    public class Token {
        private final Instant expiry;
        private final String username;
        private final Map<String, String> attributes;

        public Token(final Instant expiry, final String username) {
            this.expiry = expiry;
            this.username = username;
            this.attributes = new ConcurrentHashMap<>();
        }

        public Instant getExpiry() {
            return expiry;
        }

        public String getUsername() {
            return username;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(final Map<String, String> attrs) {
            this.attributes.putAll(attrs);
        }
    }
}
