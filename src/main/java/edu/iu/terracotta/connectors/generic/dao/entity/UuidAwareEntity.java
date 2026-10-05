package edu.iu.terracotta.connectors.generic.dao.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

/**
 * Adds a stable, non-sequential external identifier to entities that already have their own
 * differently-named {@code @Id} field (e.g. {@code Submission.submissionId}) and so can't extend
 * {@link BaseUuidEntity} directly - that class also declares an {@code id} field, which would
 * collide. This provides only the {@code uuid} half of that same pattern.
 */
@Getter
@Setter
@MappedSuperclass
public class UuidAwareEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    protected UUID uuid;

    @PrePersist
    protected void prePersistUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

}
