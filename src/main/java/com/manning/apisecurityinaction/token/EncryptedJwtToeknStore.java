package com.manning.apisecurityinaction.token;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import javax.crypto.SecretKey;
import spark.Request;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

public class EncryptedJwtToeknStore implements TokenStore {

    private final SecretKey secretKey;
    private final DatabaseTokenStore tokenAllowList;

    public EncryptedJwtToeknStore(final SecretKey secretKey, final DatabaseTokenStore tokenAllowList) {
        this.secretKey = secretKey;
        this.tokenAllowList = tokenAllowList;
    }

    @Override
    public String create(final Request request, final Token token) {
        final var allowListToken = new Token(token.getExpiry(), token.getUsername());
        final var jwtId = tokenAllowList.create(request, allowListToken);
        final var claimsBuilder = new JWTClaimsSet.Builder()
                .jwtID(jwtId)
                .subject(token.getUsername())
                .audience("https://localhost:4567")
                .expirationTime(Date.from(token.getExpiry()));
        token.getAttributes().forEach(claimsBuilder::claim);

        final var header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A128CBC_HS256);
        final var jwt = new EncryptedJWT(header, claimsBuilder.build());

        try {
            final var encrypter = new DirectEncrypter(this.secretKey);
            jwt.encrypt(encrypter);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

        return jwt.serialize();
    }

    @Override
    public Optional<Token> read(final Request request, final String tokenId) {
        try {
            final var jwt = EncryptedJWT.parse(tokenId);
            final var decryptor = new DirectDecrypter(this.secretKey);
            jwt.decrypt(decryptor);

            final var claims = jwt.getJWTClaimsSet();
            if (!claims.getAudience().contains("http://localhost:4567")) {
                return Optional.empty();
            }

            final var expiry = claims.getExpirationTime().toInstant();
            final var subject = claims.getSubject();
            final var token = new Token(expiry, subject);
            final var ignore = Set.of("exp", "sub", "aud");
            final var attrsMap = new HashMap<String, String>();
            for (final String attr : claims.getClaims().keySet()) {
                if (ignore.contains(attr)) {
                    continue;
                }
                attrsMap.put(attr, claims.getStringClaim(attr));
            }
            token.setAttributes(attrsMap);

            return Optional.of(token);
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }

    @Override
    public void revoke(final Request request, final String tokenId) {
        try {
            final var jwt = EncryptedJWT.parse(tokenId);
            final var decryptor = new DirectDecrypter(this.secretKey);
            jwt.decrypt(decryptor);
            final var claims = jwt.getJWTClaimsSet();

            tokenAllowList.revoke(request, claims.getJWTID());
        } catch (ParseException | JOSEException e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
}
