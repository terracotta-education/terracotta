package edu.iu.terracotta.dao.entity.messaging.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;

public class MessageContainerTest {

    @Test
    public void testGetExposureIdDelegatesToExposure() {
        Exposure exposure = Exposure.builder().exposureId(42L).build();
        MessageContainer messageContainer = MessageContainer.builder().exposure(exposure).build();

        assertEquals(42L, messageContainer.getExposureId());
    }

    @Test
    public void testGetStatusDelegatesToConfiguration() {
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .status(MessageStatus.SENT)
            .build();
        MessageContainer messageContainer = MessageContainer.builder().configuration(configuration).build();

        assertEquals(MessageStatus.SENT, messageContainer.getStatus());
    }

    @Test
    public void testGetOrderDelegatesToConfiguration() {
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .containerOrder(3)
            .build();
        MessageContainer messageContainer = MessageContainer.builder().configuration(configuration).build();

        assertEquals(3, messageContainer.getOrder());
    }

    @Test
    public void testGetReplyToDelegatesToConfiguration() {
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder().build();
        MessageContainer messageContainer = MessageContainer.builder().configuration(configuration).build();

        assertEquals(configuration.getReplyTo(), messageContainer.getReplyTo());
        assertTrue(messageContainer.getReplyTo().isEmpty());
    }

    @Test
    public void testGetSendAtDelegatesToConfiguration() {
        Timestamp sendAt = new Timestamp(5000L);
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .sendAt(sendAt)
            .build();
        MessageContainer messageContainer = MessageContainer.builder().configuration(configuration).build();

        assertEquals(sendAt, messageContainer.getSendAt());
    }

    @Test
    public void testGetSendAtTimezoneOffsetReturnsZeroWhenNull() {
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .sendAtTimezoneOffset(null)
            .build();
        MessageContainer messageContainer = MessageContainer.builder().configuration(configuration).build();

        assertEquals(0, messageContainer.getSendAtTimezoneOffset());
    }

    @Test
    public void testGetSendAtTimezoneOffsetReturnsConfiguredValueWhenPresent() {
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .sendAtTimezoneOffset(-300)
            .build();
        MessageContainer messageContainer = MessageContainer.builder().configuration(configuration).build();

        assertEquals(-300, messageContainer.getSendAtTimezoneOffset());
    }

    @Test
    public void testIsSingleVersionFalseWhenNoMessages() {
        MessageContainer messageContainer = MessageContainer.builder().build();

        assertTrue(messageContainer.getMessages().isEmpty());
        assertFalse(messageContainer.isSingleVersion());
    }

    @Test
    public void testIsSingleVersionTrueWhenExactlyOneMessage() {
        MessageContainer messageContainer = MessageContainer.builder()
            .messages(List.of(Message.builder().build()))
            .build();

        assertTrue(messageContainer.isSingleVersion());
    }

    @Test
    public void testIsSingleVersionFalseWhenMultipleMessages() {
        MessageContainer messageContainer = MessageContainer.builder()
            .messages(List.of(Message.builder().build(), Message.builder().build()))
            .build();

        assertFalse(messageContainer.isSingleVersion());
    }

}
