package com.manning.apisecurityinaction.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import spark.Request;

import java.sql.Date;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Optional;

public class SignedJwtTokenStore implements AuthenticatedTokenStore {
    private final JWSSigner signer;
    private final JWSVerifier verifier;
    private final JWSAlgorithm algorithm;
    private final String audience;

    public SignedJwtTokenStore(final JWSSigner signer, final JWSVerifier verifier, final JWSAlgorithm algorithm, final String audience) {
        this.signer = signer;
        this.verifier = verifier;
        this.algorithm = algorithm;
        this.audience = audience;
    }

    @Override
    public String create(final Request request, final Token token) {
        final var claimSet = new JWTClaimsSet.Builder()
                .subject(token.getUsername())
                .audience(this.audience)
                .expirationTime(Date.from(token.getExpiry()))
                .claim("attrs", token.getAttributes())
                .build();
        final var header = new JWSHeader(JWSAlgorithm.HS256);
        final var jwt = new SignedJWT(header, claimSet);
        try {
            jwt.sign(signer);

            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Token> read(final Request request, final String tokenId) {
        try {
            final var jwt = SignedJWT.parse(tokenId);
            if (!jwt.verify(this.verifier)) {
                throw new JOSEException("Invalid signature");
            }

            final var claims = jwt.getJWTClaimsSet();
            if (!claims.getAudience().contains(this.audience)) {
                throw new JOSEException("Incorrect audience");
            }

            final var expiry = claims.getExpirationTime().toInstant();
            final var subject = claims.getSubject();
            final var token = new Token(expiry, subject);
            final var attrs = claims.getJSONObjectClaim("attrs");
            final var attrsMap = new HashMap<String, String>();
            attrs.forEach((key, value) -> attrsMap.put(key, (String) value));
            token.setAttributes(attrsMap);

            return Optional.of(token);
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }

    @Override
    public void revoke(final Request request, final String tokenId) {

    }
}
