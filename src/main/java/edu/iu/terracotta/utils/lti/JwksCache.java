package edu.iu.terracotta.utils.lti;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

/**
 * Caches a platform's JWKS by endpoint URL so it isn't re-fetched over the network on every
 * single LTI launch. A key ID that isn't found in the cached set forces one bypass-cache
 * refetch, so a platform rotating its keys is still picked up without waiting out the full TTL.
 */
public final class JwksCache {

    private static final long CACHE_DURATION_SECONDS = 30 * 60;
    private static final Map<String, CachedJwks> CACHE = new ConcurrentHashMap<>();

    private JwksCache() {}

    public static JWK getKey(String jwksEndpoint, String keyId) throws IOException, ParseException, URISyntaxException {
        JWK jwk = get(jwksEndpoint).getKeyByKeyId(keyId);

        if (jwk != null) {
            return jwk;
        }

        return fetch(jwksEndpoint).getKeyByKeyId(keyId);
    }

    private static JWKSet get(String jwksEndpoint) throws IOException, ParseException, URISyntaxException {
        CachedJwks cached = CACHE.get(jwksEndpoint);

        if (cached != null && cached.isFresh()) {
            return cached.jwkSet();
        }

        return fetch(jwksEndpoint);
    }

    private static JWKSet fetch(String jwksEndpoint) throws IOException, ParseException, URISyntaxException {
        JWKSet jwkSet = JWKSet.load(new URI(jwksEndpoint).toURL());
        CACHE.put(jwksEndpoint, new CachedJwks(jwkSet, Instant.now().plusSeconds(CACHE_DURATION_SECONDS)));

        return jwkSet;
    }

    private record CachedJwks(JWKSet jwkSet, Instant expiresAt) {
        boolean isFresh() {
            return Instant.now().isBefore(expiresAt);
        }
    }

}
