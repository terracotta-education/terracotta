package edu.iu.terracotta.security.pii;

/**
 * An entity whose (encrypted) email is matched in queries by a keyed hash of it, kept current by
 * {@link EmailHashListener}.
 */
public interface HashedEmail {

    String getEmail();

    void setEmailHash(String emailHash);

}
