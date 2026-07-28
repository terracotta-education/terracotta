package edu.iu.terracotta.service.app.messaging.impl.conditional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextResult;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextResultDto;

public class MessageConditionalTextResultServiceImplTest extends BaseTest {

    @InjectMocks private MessageConditionalTextResultServiceImpl conditionalTextResultService;

    private MessageConditionalText conditionalText;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        conditionalText = MessageConditionalText.builder()
            .label("label")
            .build();
        conditionalText.setUuid(UUID.randomUUID());
    }

    @Test
    public void testCreate() {
        MessageConditionalTextResultDto dto = MessageConditionalTextResultDto.builder()
            .html("<p>hello</p>")
            .build();

        conditionalTextResultService.create(dto, conditionalText);

        assertEquals("<p>hello</p>", conditionalText.getResult().getHtml());
        assertSame(conditionalText, conditionalText.getResult().getConditionalText());
    }

    @Test
    public void testUpdate() {
        MessageConditionalTextResult existing = MessageConditionalTextResult.builder()
            .conditionalText(conditionalText)
            .html("old")
            .build();
        conditionalText.setResult(existing);

        MessageConditionalTextResultDto dto = MessageConditionalTextResultDto.builder()
            .html("new")
            .build();

        conditionalTextResultService.update(dto, conditionalText);

        assertEquals("new", conditionalText.getResult().getHtml());
        assertSame(existing, conditionalText.getResult());
    }

    @Test
    public void testDuplicate() {
        MessageConditionalTextResult original = MessageConditionalTextResult.builder()
            .conditionalText(conditionalText)
            .html("original html")
            .build();

        MessageConditionalText newConditionalText = MessageConditionalText.builder()
            .label("new")
            .build();

        conditionalTextResultService.duplicate(original, newConditionalText);

        assertEquals("original html", newConditionalText.getResult().getHtml());
        assertSame(newConditionalText, newConditionalText.getResult().getConditionalText());
    }

    @Test
    public void testToDto() {
        UUID resultUuid = UUID.randomUUID();
        MessageConditionalTextResult result = MessageConditionalTextResult.builder()
            .conditionalText(conditionalText)
            .html("some html")
            .build();
        result.setUuid(resultUuid);

        MessageConditionalTextResultDto dto = conditionalTextResultService.toDto(result);

        assertEquals(resultUuid, dto.getId());
        assertEquals(conditionalText.getUuid(), dto.getConditionalTextId());
        assertEquals("some html", dto.getHtml());
    }

    @Test
    public void testFromDtoNullDto() {
        MessageConditionalTextResult result = MessageConditionalTextResult.builder()
            .conditionalText(conditionalText)
            .html("unchanged")
            .build();

        MessageConditionalTextResult returned = conditionalTextResultService.fromDto(null, result);

        assertSame(result, returned);
        assertEquals("unchanged", returned.getHtml());
    }

    @Test
    public void testFromDto() {
        UUID id = UUID.randomUUID();
        MessageConditionalTextResultDto dto = MessageConditionalTextResultDto.builder()
            .id(id)
            .html("new html")
            .build();
        MessageConditionalTextResult result = MessageConditionalTextResult.builder()
            .conditionalText(conditionalText)
            .build();

        MessageConditionalTextResult returned = conditionalTextResultService.fromDto(dto, result);

        assertEquals(id, returned.getUuid());
        assertEquals("new html", returned.getHtml());
        assertSame(conditionalText, returned.getConditionalText());
    }

}
