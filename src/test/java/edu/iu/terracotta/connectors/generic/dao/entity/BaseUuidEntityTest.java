package edu.iu.terracotta.connectors.generic.dao.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * {@link BaseUuidEntity} has a hand-written, protected {@code prePersist()} lifecycle callback
 * ({@code @PrePersist}) that assigns a random {@link UUID} only when {@code uuid} has not already
 * been set (early-return otherwise). The test lives in the same package as the production class so
 * it can invoke the protected method directly without reflection.
 */
public class BaseUuidEntityTest {

    @Test
    public void testPrePersistAssignsUuidWhenNull() {
        BaseUuidEntity baseUuidEntity = new BaseUuidEntity();

        assertNull(baseUuidEntity.getUuid());

        baseUuidEntity.prePersist();

        assertNotNull(baseUuidEntity.getUuid());
    }

    @Test
    public void testPrePersistDoesNotOverwriteExistingUuid() {
        BaseUuidEntity baseUuidEntity = new BaseUuidEntity();
        UUID uuid = UUID.randomUUID();
        baseUuidEntity.setUuid(uuid);

        baseUuidEntity.prePersist();

        assertEquals(uuid, baseUuidEntity.getUuid());
    }

    @Test
    public void testPrePersistGeneratesDifferentUuidsForDifferentInstances() {
        BaseUuidEntity baseUuidEntity1 = new BaseUuidEntity();
        BaseUuidEntity baseUuidEntity2 = new BaseUuidEntity();

        baseUuidEntity1.prePersist();
        baseUuidEntity2.prePersist();

        assertNotNull(baseUuidEntity1.getUuid());
        assertNotNull(baseUuidEntity2.getUuid());
        assertNotEquals(baseUuidEntity1.getUuid(), baseUuidEntity2.getUuid());
    }

}
