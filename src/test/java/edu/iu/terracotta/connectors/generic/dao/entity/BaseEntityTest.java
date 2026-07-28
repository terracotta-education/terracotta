package edu.iu.terracotta.connectors.generic.dao.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

/**
 * {@link BaseEntity} is a Lombok {@code @Getter @Setter} {@code @MappedSuperclass}. These tests
 * exercise its hand-written, package-private lifecycle callbacks: {@code preCreate()}
 * ({@code @PrePersist}), which sets both {@code createdAt} and {@code updatedAt} to the same new
 * timestamp, and {@code preUpdate()} ({@code @PreUpdate}), which updates only {@code updatedAt}
 * without touching {@code createdAt}. The test lives in the same package as the production class
 * so it can invoke the package-private methods directly without reflection.
 */
public class BaseEntityTest {

    @Test
    public void testPreCreateSetsCreatedAtAndUpdatedAtToSameTimestamp() {
        BaseEntity baseEntity = new BaseEntity();

        assertNull(baseEntity.getCreatedAt());
        assertNull(baseEntity.getUpdatedAt());

        baseEntity.preCreate();

        assertNotNull(baseEntity.getCreatedAt());
        assertNotNull(baseEntity.getUpdatedAt());
        assertEquals(baseEntity.getCreatedAt(), baseEntity.getUpdatedAt());
    }

    @Test
    public void testPreUpdateChangesUpdatedAtButNotCreatedAt() throws InterruptedException {
        BaseEntity baseEntity = new BaseEntity();
        baseEntity.preCreate();

        Timestamp createdAt = baseEntity.getCreatedAt();
        Timestamp originalUpdatedAt = baseEntity.getUpdatedAt();

        // ensure the clock advances so the new updatedAt timestamp is distinguishable
        Thread.sleep(10);
        baseEntity.preUpdate();

        assertEquals(createdAt, baseEntity.getCreatedAt());
        assertNotNull(baseEntity.getUpdatedAt());
        assertNotEquals(originalUpdatedAt, baseEntity.getUpdatedAt());
    }

}
