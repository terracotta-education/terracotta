package edu.iu.terracotta.runner.export.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

public class ExperimentDataExportScheduleMessageTest {

    @Test
    public void testBuilderAndGetters() {
        Timestamp deletedAt = new Timestamp(2000L);

        ExperimentDataExportScheduleMessage message = ExperimentDataExportScheduleMessage.builder()
            .id(1L)
            .fileName("export.csv")
            .fileUri("s3://bucket/export.csv")
            .deletedAt(deletedAt)
            .build();

        assertEquals(1L, message.getId());
        assertEquals("export.csv", message.getFileName());
        assertEquals("s3://bucket/export.csv", message.getFileUri());
        assertEquals(deletedAt, message.getDeletedAt());
        assertNull(message.getErrors());

        message.setFileName("export2.csv");
        assertEquals("export2.csv", message.getFileName());
    }

    @Test
    public void testAddErrorInitializesListWhenNull() {
        ExperimentDataExportScheduleMessage message = ExperimentDataExportScheduleMessage.builder().build();

        message.addError("first error");

        assertNotNull(message.getErrors());
        assertEquals(1, message.getErrors().size());
        assertEquals("first error", message.getErrors().get(0));
    }

    @Test
    public void testAddErrorAppendsToExistingList() {
        ExperimentDataExportScheduleMessage message = ExperimentDataExportScheduleMessage.builder().build();

        message.addError("first error");
        message.addError("second error");

        assertEquals(2, message.getErrors().size());
        assertEquals("second error", message.getErrors().get(1));
    }

}
