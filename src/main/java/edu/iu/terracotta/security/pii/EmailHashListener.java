package edu.iu.terracotta.security.pii;

import org.springframework.stereotype.Component;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;

/**
 * Keeps an entity's email hash in step with its (encrypted) email, so the email can still be
 * looked up and grouped by in queries.
 */
@Component
@RequiredArgsConstructor
public class EmailHashListener {

    private final PiiCipher piiCipher;

    @PrePersist
    @PreUpdate
    public void updateEmailHash(Object entity) {
        if (entity instanceof HashedEmail hashedEmail) {
            hashedEmail.setEmailHash(piiCipher.hashEmail(hashedEmail.getEmail()));
        }
    }

}
