package edu.iu.terracotta.service.app.messaging.impl.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextResult;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentDto;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextDto;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextDto;
import edu.iu.terracotta.dao.repository.messaging.content.MessageContentRepository;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextService;
import edu.iu.terracotta.service.app.messaging.MessageContentAttachmentService;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextService;

public class MessageContentServiceImplTest extends BaseTest {

    @Mock private MessageContentRepository contentRepository;
    @Mock private MessageConditionalTextService conditionalTextService;
    @Mock private MessageContentAttachmentService contentAttachmentService;
    @Mock private MessagePipedTextService pipedTextService;

    @InjectMocks private MessageContentServiceImpl contentService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testCreate() {
        Message message = Message.builder().build();

        contentService.create(message);

        assertEquals(message, message.getContent().getMessage());
    }

    @Test
    public void testUpdate() {
        MessageContent content = MessageContent.builder().html("old").build();
        Message message = Message.builder().content(content).build();
        MessageContentDto dto = MessageContentDto.builder().html("new").attachments(List.of()).conditionalTexts(List.of()).build();

        contentService.update(dto, message);

        assertEquals("new", message.getContent().getHtml());
        verify(contentAttachmentService).update(dto.getAttachments(), content);
        verify(conditionalTextService).upsert(dto.getConditionalTexts(), content);
        verify(pipedTextService).upsert(dto.getPipedText(), content);
    }

    @Test
    public void testDuplicate() throws MessageBodyParseException {
        MessageContent content = MessageContent.builder().html("plain text").attachments(List.of()).conditionalTexts(new ArrayList<>()).build();
        Message message = Message.builder().build();

        contentService.duplicate(content, message);

        assertEquals("plain text", message.getContent().getHtml());
        verify(contentAttachmentService).duplicate(eq(content.getAttachments()), any(MessageContent.class));
        verify(conditionalTextService).duplicate(eq(content.getConditionalTexts()), any(MessageContent.class));
        verify(pipedTextService).duplicate(eq(content.getPipedText()), any(MessageContent.class));
    }

    @Test
    public void testToDtoNullReturnsNull() {
        assertNull(contentService.toDto(null));
    }

    @Test
    public void testToDtoMapsFields() {
        Message message = mock(Message.class);
        UUID messageUuid = UUID.randomUUID();
        when(message.getUuid()).thenReturn(messageUuid);
        MessageContent content = MessageContent.builder().html("body").message(message).attachments(List.of()).conditionalTexts(List.of()).build();
        content.setUuid(UUID.randomUUID());

        when(contentAttachmentService.toDto(anyList())).thenReturn(List.of());
        when(conditionalTextService.toDto(anyList())).thenReturn(List.of());
        when(pipedTextService.toDto(any())).thenReturn(mock(MessagePipedTextDto.class));

        MessageContentDto dto = contentService.toDto(content);

        assertEquals(content.getUuid(), dto.getId());
        assertEquals(messageUuid, dto.getMessageId());
        assertEquals("body", dto.getHtml());
    }

    @Test
    public void testFromDtoMapsFieldsAndDelegates() {
        MessageContent content = MessageContent.builder().build();
        MessageContentDto dto = MessageContentDto.builder().html("new html").attachments(List.of()).conditionalTexts(List.of()).build();

        MessageContent result = contentService.fromDto(dto, content);

        assertEquals("new html", result.getHtml());
        verify(contentAttachmentService).update(dto.getAttachments(), content);
        verify(conditionalTextService).upsert(dto.getConditionalTexts(), content);
        verify(pipedTextService).upsert(dto.getPipedText(), content);
    }

    @Test
    public void testUpdatePlaceholdersBlankHtmlIsNoOp() throws MessageBodyParseException {
        MessageContent content = MessageContent.builder().html("  ").build();

        contentService.updatePlaceholders(content, false);

        assertEquals("  ", content.getHtml());
        verify(contentRepository, never()).save(any(MessageContent.class));
    }

    @Test
    public void testUpdatePlaceholdersValidConditionalTextUpdatesDataId() throws MessageBodyParseException {
        MessageConditionalTextResult result = MessageConditionalTextResult.builder().build();
        MessageConditionalText conditionalText = MessageConditionalText.builder().label("mylabel").result(result).build();
        conditionalText.setUuid(UUID.randomUUID());
        MessageContent content = MessageContent.builder()
            .html("<conditional-text data-id=\"old-id\" data-label=\"conditional text: mylabel\" onclick=\"go(old-id)\"></conditional-text>")
            .conditionalTexts(new ArrayList<>(List.of(conditionalText)))
            .pipedText(MessagePipedText.builder().build())
            .build();

        contentService.updatePlaceholders(content, false);

        org.jsoup.nodes.Document parsed = Jsoup.parse(content.getHtml());
        org.jsoup.nodes.Element element = parsed.getElementsByTag("conditional-text").first();
        assertEquals(conditionalText.getUuid().toString(), element.attr("data-id"));
        assertTrue(element.attr("onclick").contains(conditionalText.getUuid().toString()));
    }

    @Test
    public void testUpdatePlaceholdersInvalidConditionalTextMarksInvalid() throws MessageBodyParseException {
        MessageContent content = MessageContent.builder()
            .html("<conditional-text data-id=\"old-id\" data-label=\"conditional text: unknown\"></conditional-text>")
            .conditionalTexts(new ArrayList<>())
            .pipedText(MessagePipedText.builder().build())
            .build();

        contentService.updatePlaceholders(content, false);

        org.jsoup.nodes.Document parsed = Jsoup.parse(content.getHtml());
        org.jsoup.nodes.Element element = parsed.getElementsByTag("conditional-text").first();
        assertEquals("{{ INVALID conditional text: unknown }}", element.text());
        assertEquals("", element.attr("data-id"));
    }

    @Test
    public void testUpdatePlaceholdersValidPipedTextUpdatesDataId() throws MessageBodyParseException {
        MessagePipedTextItem item = MessagePipedTextItem.builder().key("mykey").build();
        item.setUuid(UUID.randomUUID());
        MessageContent content = MessageContent.builder()
            .html("<piped-text data-id=\"old-id\" data-label=\"piped text: mykey\"></piped-text>")
            .conditionalTexts(new ArrayList<>())
            .pipedText(MessagePipedText.builder().items(List.of(item)).build())
            .build();

        contentService.updatePlaceholders(content, false);

        org.jsoup.nodes.Document parsed = Jsoup.parse(content.getHtml());
        org.jsoup.nodes.Element element = parsed.getElementsByTag("piped-text").first();
        assertEquals(item.getUuid().toString(), element.attr("data-id"));
        assertFalse(element.hasClass("invalid-piped-text"));
    }

    @Test
    public void testUpdatePlaceholdersInvalidPipedTextMarksInvalidAndAddsCssClass() throws MessageBodyParseException {
        MessageContent content = MessageContent.builder()
            .html("<piped-text data-id=\"old-id\" data-label=\"piped text: unknown\"></piped-text>")
            .conditionalTexts(new ArrayList<>())
            .pipedText(MessagePipedText.builder().items(List.of()).build())
            .build();

        contentService.updatePlaceholders(content, false);

        org.jsoup.nodes.Document parsed = Jsoup.parse(content.getHtml());
        org.jsoup.nodes.Element element = parsed.getElementsByTag("piped-text").first();
        assertTrue(element.hasClass("invalid-piped-text"));
        assertTrue(element.attr("data-label").startsWith("INVALID"));
    }

    @Test
    public void testUpdatePlaceholdersProcessesPipedTextWithinConditionalTextResult() throws MessageBodyParseException {
        MessagePipedTextItem item = MessagePipedTextItem.builder().key("mykey").build();
        item.setUuid(UUID.randomUUID());
        MessageConditionalTextResult result = MessageConditionalTextResult.builder()
            .html("<piped-text data-id=\"old-id\" data-label=\"piped text: mykey\"></piped-text>")
            .build();
        MessageConditionalText conditionalText = MessageConditionalText.builder().label("mylabel").result(result).build();
        MessageContent content = MessageContent.builder()
            .html("<p>Body</p>")
            .conditionalTexts(new ArrayList<>(List.of(conditionalText)))
            .pipedText(MessagePipedText.builder().items(List.of(item)).build())
            .build();

        contentService.updatePlaceholders(content, false);

        org.jsoup.nodes.Document parsedResult = Jsoup.parse(conditionalText.getResult().getHtml());
        org.jsoup.nodes.Element element = parsedResult.getElementsByTag("piped-text").first();
        assertEquals(item.getUuid().toString(), element.attr("data-id"));
    }

    @Test
    public void testUpdatePlaceholdersSavesWhenSaveTrue() throws MessageBodyParseException {
        MessageContent content = MessageContent.builder()
            .html("<p>Body</p>")
            .conditionalTexts(new ArrayList<>())
            .pipedText(MessagePipedText.builder().build())
            .build();
        when(contentRepository.save(content)).thenReturn(content);

        contentService.updatePlaceholders(content, true);

        verify(contentRepository).save(content);
    }

    @Test
    public void testUpdatePlaceholdersWithDtoOverloadUpdatesHtmlAndPipedTextAndConditionalTexts() throws MessageBodyParseException {
        MessageContent content = MessageContent.builder().build();
        MessageContentDto contentDto = MessageContentDto.builder()
            .html("<p>Updated</p>")
            .pipedText(mock(MessagePipedTextDto.class))
            .conditionalTexts(List.of(mock(MessageConditionalTextDto.class)))
            .build();

        when(pipedTextService.fromDto(eq(contentDto.getPipedText()), any(MessagePipedText.class), eq(true)))
            .thenReturn(MessagePipedText.builder().items(List.of()).build());
        when(conditionalTextService.fromDto(eq(contentDto.getConditionalTexts()), eq(content), eq(true), eq(true)))
            .thenReturn(new ArrayList<>());
        when(conditionalTextService.toDto(anyList())).thenReturn(List.of());

        MessageContentDto result = contentService.updatePlaceholders(content, contentDto);

        assertEquals("<p>Updated</p>", content.getHtml());
        assertEquals("<p>Updated</p>", result.getHtml());
        assertTrue(result.getConditionalTexts().isEmpty());
    }

    @Test
    public void testPrepareBodyHtmlForExportBlankReturnsAsIs() {
        assertEquals("", contentService.prepareBodyHtmlForExport(""));
        assertNull(contentService.prepareBodyHtmlForExport(null));
    }

    @Test
    public void testPrepareBodyHtmlForExportReplacesElementsWithText() {
        String body = "<conditional-text>Hello</conditional-text><piped-text>World</piped-text>";

        String result = contentService.prepareBodyHtmlForExport(body);

        assertFalse(result.contains("<conditional-text"));
        assertFalse(result.contains("<piped-text"));
        assertTrue(result.contains("Hello"));
        assertTrue(result.contains("World"));
    }

}
