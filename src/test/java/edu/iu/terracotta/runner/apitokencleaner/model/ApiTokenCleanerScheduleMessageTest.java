package edu.iu.terracotta.runner.apitokencleaner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;

public class ApiTokenCleanerScheduleMessageTest {

    @Test
    public void testBuilderAndGetters() {
        Timestamp expiresAt = new Timestamp(1000L);
        Timestamp deletedAt = new Timestamp(2000L);

        ApiTokenCleanerScheduleMessage message = ApiTokenCleanerScheduleMessage.builder()
            .id(1L)
            .lmsUserName("user-1")
            .userId(2L)
            .lmsConnector(LmsConnector.CANVAS)
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .expiresAt(expiresAt)
            .deletedAt(deletedAt)
            .build();

        assertEquals(1L, message.getId());
        assertEquals("user-1", message.getLmsUserName());
        assertEquals(2L, message.getUserId());
        assertEquals(LmsConnector.CANVAS, message.getLmsConnector());
        assertEquals("access-token", message.getAccessToken());
        assertEquals("refresh-token", message.getRefreshToken());
        assertEquals(expiresAt, message.getExpiresAt());
        assertEquals(deletedAt, message.getDeletedAt());
        assertNull(message.getErrors());

        message.setLmsUserName("user-2");
        assertEquals("user-2", message.getLmsUserName());
    }

    @Test
    public void testAddErrorInitializesListWhenNull() {
        ApiTokenCleanerScheduleMessage message = ApiTokenCleanerScheduleMessage.builder().build();

        message.addError("first error");

        assertNotNull(message.getErrors());
        assertEquals(1, message.getErrors().size());
        assertEquals("first error", message.getErrors().get(0));
    }

    @Test
    public void testAddErrorAppendsToExistingList() {
        ApiTokenCleanerScheduleMessage message = ApiTokenCleanerScheduleMessage.builder().build();

        message.addError("first error");
        message.addError("second error");

        assertEquals(2, message.getErrors().size());
        assertEquals("second error", message.getErrors().get(1));
    }

}
