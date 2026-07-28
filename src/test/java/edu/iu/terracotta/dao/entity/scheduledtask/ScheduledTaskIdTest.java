package edu.iu.terracotta.dao.entity.scheduledtask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * {@link ScheduledTaskId} is a plain {@code @Embeddable} class with Lombok
 * {@code @EqualsAndHashCode} generated over its two public fields. These tests exercise the
 * generated {@code equals()}/{@code hashCode()} contract: field-by-field equality, reflexivity,
 * and safe handling of {@code null} and mismatched-type comparisons.
 */
public class ScheduledTaskIdTest {

    @Test
    public void testEqualsAndHashCodeWithIdenticalFields() {
        ScheduledTaskId scheduledTaskId1 = new ScheduledTaskId();
        scheduledTaskId1.taskName = "task-name";
        scheduledTaskId1.taskInstance = "task-instance";

        ScheduledTaskId scheduledTaskId2 = new ScheduledTaskId();
        scheduledTaskId2.taskName = "task-name";
        scheduledTaskId2.taskInstance = "task-instance";

        assertEquals(scheduledTaskId1, scheduledTaskId2);
        assertEquals(scheduledTaskId1.hashCode(), scheduledTaskId2.hashCode());
    }

    @Test
    public void testNotEqualWhenTaskNameDiffers() {
        ScheduledTaskId scheduledTaskId1 = new ScheduledTaskId();
        scheduledTaskId1.taskName = "task-name-1";
        scheduledTaskId1.taskInstance = "task-instance";

        ScheduledTaskId scheduledTaskId2 = new ScheduledTaskId();
        scheduledTaskId2.taskName = "task-name-2";
        scheduledTaskId2.taskInstance = "task-instance";

        assertNotEquals(scheduledTaskId1, scheduledTaskId2);
    }

    @Test
    public void testNotEqualWhenTaskInstanceDiffers() {
        ScheduledTaskId scheduledTaskId1 = new ScheduledTaskId();
        scheduledTaskId1.taskName = "task-name";
        scheduledTaskId1.taskInstance = "task-instance-1";

        ScheduledTaskId scheduledTaskId2 = new ScheduledTaskId();
        scheduledTaskId2.taskName = "task-name";
        scheduledTaskId2.taskInstance = "task-instance-2";

        assertNotEquals(scheduledTaskId1, scheduledTaskId2);
    }

    @Test
    public void testNotEqualToNull() {
        ScheduledTaskId scheduledTaskId = new ScheduledTaskId();
        scheduledTaskId.taskName = "task-name";
        scheduledTaskId.taskInstance = "task-instance";

        assertNotEquals(null, scheduledTaskId);
    }

    @Test
    public void testNotEqualToDifferentType() {
        ScheduledTaskId scheduledTaskId = new ScheduledTaskId();
        scheduledTaskId.taskName = "task-name";
        scheduledTaskId.taskInstance = "task-instance";

        assertNotEquals(scheduledTaskId, "a string");
        assertNotEquals(scheduledTaskId, new Object());
    }

    @Test
    public void testEqualToItself() {
        ScheduledTaskId scheduledTaskId = new ScheduledTaskId();
        scheduledTaskId.taskName = "task-name";
        scheduledTaskId.taskInstance = "task-instance";

        assertEquals(scheduledTaskId, scheduledTaskId);
    }

}
