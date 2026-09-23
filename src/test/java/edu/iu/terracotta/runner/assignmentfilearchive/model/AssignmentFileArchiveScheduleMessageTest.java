package edu.iu.terracotta.runner.assignmentfilearchive.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

public class AssignmentFileArchiveScheduleMessageTest {

    @Test
    public void testBuilderAndGetters() {
        Timestamp deletedAt = new Timestamp(2000L);

        AssignmentFileArchiveScheduleMessage message = AssignmentFileArchiveScheduleMessage.builder()
            .id(1L)
            .fileName("file.zip")
            .fileUri("s3://bucket/file.zip")
            .deletedAt(deletedAt)
            .build();

        assertEquals(1L, message.getId());
        assertEquals("file.zip", message.getFileName());
        assertEquals("s3://bucket/file.zip", message.getFileUri());
        assertEquals(deletedAt, message.getDeletedAt());
        assertNull(message.getErrors());

        message.setFileName("file2.zip");
        assertEquals("file2.zip", message.getFileName());
    }

    @Test
    public void testAddErrorInitializesListWhenNull() {
        AssignmentFileArchiveScheduleMessage message = AssignmentFileArchiveScheduleMessage.builder().build();

        message.addError("first error");

        assertNotNull(message.getErrors());
        assertEquals(1, message.getErrors().size());
        assertEquals("first error", message.getErrors().get(0));
    }

    @Test
    public void testAddErrorAppendsToExistingList() {
        AssignmentFileArchiveScheduleMessage message = AssignmentFileArchiveScheduleMessage.builder().build();

        message.addError("first error");
        message.addError("second error");

        assertEquals(2, message.getErrors().size());
        assertEquals("second error", message.getErrors().get(1));
    }

}
