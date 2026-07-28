package edu.iu.terracotta.utils.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

public class JwksCacheTest {

    private static RSAPublicKey generatePublicKey() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        return (RSAPublicKey) keyPair.getPublic();
    }

    private static String writeJwksFile(File file, String... keyIds) throws Exception {
        List<JWK> jwks = new java.util.ArrayList<>();

        for (String keyId : keyIds) {
            jwks.add(new RSAKey.Builder(generatePublicKey()).keyID(keyId).build());
        }

        Files.writeString(file.toPath(), new JWKSet(jwks).toString());

        return file.toURI().toString();
    }

    @Test
    public void testGetKeyCachesJwksAcrossCalls() throws Exception {
        File file = File.createTempFile("jwks-cache-test", ".json");
        file.deleteOnExit();
        String endpoint = writeJwksFile(file, "kid-1");

        JWK first = JwksCache.getKey(endpoint, "kid-1");
        assertNotNull(first);

        // delete the backing file - if getKey() were still hitting the network/filesystem
        // instead of the cache, this second call would fail
        assertEquals(true, file.delete());

        JWK second = JwksCache.getKey(endpoint, "kid-1");
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
    public void testGetKeyForcesRefetchWhenKeyIdNotInCachedSet() throws Exception {
        File file = File.createTempFile("jwks-cache-test-rotate", ".json");
        file.deleteOnExit();
        String endpoint = writeJwksFile(file, "kid-1");

        // caches a JWKS that only contains kid-1
        assertNotNull(JwksCache.getKey(endpoint, "kid-1"));

        // simulate the platform rotating its keys: same endpoint, now serving kid-2 instead
        writeJwksFile(file, "kid-2");

        JWK rotated = JwksCache.getKey(endpoint, "kid-2");
        assertNotNull(rotated);
        assertEquals("kid-2", rotated.getKeyID());
    }

    @Test
    public void testGetKeyReturnsNullForUnknownKeyIdAfterRefetch() throws Exception {
        File file = File.createTempFile("jwks-cache-test-unknown", ".json");
        file.deleteOnExit();
        String endpoint = writeJwksFile(file, "kid-1");

        JWK result = JwksCache.getKey(endpoint, "does-not-exist");

        assertNull(result);
    }

}
