package edu.iu.terracotta.runner.distribute.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

public class ExperimentImportScheduleMessageTest {

    @Test
    public void testBuilderAndGetters() {
        Timestamp deletedAt = new Timestamp(2000L);

        ExperimentImportScheduleMessage message = ExperimentImportScheduleMessage.builder()
            .id(1L)
            .fileName("import.zip")
            .fileUri("s3://bucket/import.zip")
            .deletedAt(deletedAt)
            .build();

        assertEquals(1L, message.getId());
        assertEquals("import.zip", message.getFileName());
        assertEquals("s3://bucket/import.zip", message.getFileUri());
        assertEquals(deletedAt, message.getDeletedAt());
        assertNull(message.getErrors());

        message.setFileName("import2.zip");
        assertEquals("import2.zip", message.getFileName());
    }

    @Test
    public void testAddErrorInitializesListWhenNull() {
        ExperimentImportScheduleMessage message = ExperimentImportScheduleMessage.builder().build();

        message.addError("first error");

        assertNotNull(message.getErrors());
        assertEquals(1, message.getErrors().size());
        assertEquals("first error", message.getErrors().get(0));
    }

    @Test
    public void testAddErrorAppendsToExistingList() {
        ExperimentImportScheduleMessage message = ExperimentImportScheduleMessage.builder().build();

        message.addError("first error");
        message.addError("second error");

        assertEquals(2, message.getErrors().size());
        assertEquals("second error", message.getErrors().get(1));
    }

}
