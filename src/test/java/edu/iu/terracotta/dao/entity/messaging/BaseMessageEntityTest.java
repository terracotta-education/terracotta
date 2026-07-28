package edu.iu.terracotta.dao.entity.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * {@link BaseMessageEntity} is a Lombok {@code @Getter @Setter} {@code @MappedSuperclass}. These
 * tests exercise its hand-written, package-private {@code prePersist()} lifecycle callback, which
 * assigns a random {@link UUID} only when {@code uuid} has not already been set. The test lives in
 * the same package as the production class so it can invoke the {@code protected} method directly
 * without reflection.
 */
public class BaseMessageEntityTest {

    @Test
    public void testPrePersistAssignsUuidWhenNull() {
        BaseMessageEntity baseMessageEntity = new BaseMessageEntity();

        assertNull(baseMessageEntity.getUuid());

        baseMessageEntity.prePersist();

        assertNotNull(baseMessageEntity.getUuid());
    }

    @Test
    public void testPrePersistDoesNotOverwriteExistingUuid() {
        BaseMessageEntity baseMessageEntity = new BaseMessageEntity();
        UUID uuid = UUID.randomUUID();
        baseMessageEntity.setUuid(uuid);

        baseMessageEntity.prePersist();

        assertEquals(uuid, baseMessageEntity.getUuid());
    }

    @Test
    public void testPrePersistGeneratesDifferentUuidsForDifferentInstances() {
        BaseMessageEntity baseMessageEntity1 = new BaseMessageEntity();
        BaseMessageEntity baseMessageEntity2 = new BaseMessageEntity();

        baseMessageEntity1.prePersist();
        baseMessageEntity2.prePersist();

        assertNotNull(baseMessageEntity1.getUuid());
        assertNotNull(baseMessageEntity2.getUuid());
        assertNotEquals(baseMessageEntity1.getUuid(), baseMessageEntity2.getUuid());
    }

}
