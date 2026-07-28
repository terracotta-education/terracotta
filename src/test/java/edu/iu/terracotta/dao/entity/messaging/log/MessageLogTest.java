package edu.iu.terracotta.dao.entity.messaging.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;

/**
 * {@link MessageLog} is a Lombok {@code @Builder} JPA entity. These tests exercise its
 * hand-written {@code @Transient} getters, all of which delegate through the {@code message}
 * field (and, transitively, through the objects it returns) with no branching logic.
 */
public class MessageLogTest {

    @Mock private Message message;
    @Mock private Condition condition;
    @Mock private MessageConfiguration messageConfiguration;
    @Mock private MessageContent messageContent;

    private MessageLog messageLog;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        when(message.getCondition()).thenReturn(condition);
        when(message.getConditionId()).thenReturn(42L);
        when(condition.getName()).thenReturn("Condition A");
        when(message.getConfiguration()).thenReturn(messageConfiguration);
        when(messageConfiguration.getSubject()).thenReturn("Subject line");
        when(message.getContent()).thenReturn(messageContent);
        when(messageContent.getHtml()).thenReturn("<p>Body</p>");

        messageLog = MessageLog.builder()
            .message(message)
            .build();
    }

    @Test
    public void testDelegatingGettersReturnStubbedValues() {
        assertSame(condition, messageLog.getCondition());
        assertEquals(42L, messageLog.getConditionId());
        assertEquals("Condition A", messageLog.getConditionName());
        assertSame(messageConfiguration, messageLog.getMessageConfiguration());
        assertEquals("Subject line", messageLog.getMessageSubject());
        assertSame(messageContent, messageLog.getMessageContent());
        assertEquals("<p>Body</p>", messageLog.getMessageBody());
    }

}
