package edu.iu.terracotta.service.app.messaging.impl.conditional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextResult;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextDto;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextResultDto;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextRuleSetDto;
import edu.iu.terracotta.dao.repository.messaging.conditional.MessageConditionalTextRepository;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextResultService;
import edu.iu.terracotta.service.app.messaging.MessageConditionalTextRuleSetService;

@SuppressWarnings("unchecked")
public class MessageConditionalTextServiceImplTest extends BaseTest {

    @Mock private MessageConditionalTextRepository conditionalTextRepository;
    @Mock private MessageConditionalTextResultService conditionalTextResultService;
    @Mock private MessageConditionalTextRuleSetService conditionalTextRuleSetService;

    @InjectMocks private MessageConditionalTextServiceImpl conditionalTextService;

    private MessageContent content;
    private LtiUserEntity owner;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        owner = LtiUserEntity.builder().lmsUserId("lms-user-1").build();
        MessageContainer container = MessageContainer.builder().owner(owner).build();
        Message message = Message.builder().container(container).build();
        content = MessageContent.builder().message(message).build();
        content.setUuid(UUID.randomUUID());
    }

    @Test
    public void testCreate() {
        MessageConditionalTextResultDto resultDto = MessageConditionalTextResultDto.builder().html("html").build();
        MessageConditionalTextDto dto = MessageConditionalTextDto.builder()
            .label("label")
            .result(resultDto)
            .ruleSets(List.of())
            .build();

        conditionalTextService.create(dto, content);

        assertEquals(1, content.getConditionalTexts().size());
        MessageConditionalText created = content.getConditionalTexts().get(0);
        assertSame(content, created.getContent());
        assertEquals("label", created.getLabel());
        verify(conditionalTextRuleSetService).create(dto.getRuleSets(), created);
        verify(conditionalTextResultService).create(resultDto, created);
    }

    @Test
    public void testPost() {
        MessageConditionalTextDto dto = MessageConditionalTextDto.builder()
            .label("label")
            .build();
        MessageConditionalText saved = MessageConditionalText.builder().content(content).label("label").build();
        saved.setUuid(UUID.randomUUID());
        when(conditionalTextRepository.save(any(MessageConditionalText.class))).thenReturn(saved);
        when(conditionalTextResultService.toDto(null)).thenReturn(null);
        when(conditionalTextRuleSetService.toDto(saved.getRuleSets())).thenReturn(List.of());

        MessageConditionalTextDto result = conditionalTextService.post(dto, content);

        assertEquals(saved.getUuid(), result.getId());
        assertEquals(content.getUuid(), result.getContentId());
        verify(conditionalTextRepository).save(content.getConditionalTexts().get(0));
    }

    @Test
    public void testUpdate() {
        MessageConditionalText conditionalText = MessageConditionalText.builder().content(content).label("old").build();
        conditionalText.setUuid(UUID.randomUUID());
        MessageConditionalTextResultDto resultDto = MessageConditionalTextResultDto.builder().html("html").build();
        MessageConditionalTextDto dto = MessageConditionalTextDto.builder()
            .label("new")
            .result(resultDto)
            .ruleSets(List.of())
            .build();

        conditionalTextService.update(dto, conditionalText);

        assertEquals("new", conditionalText.getLabel());
        verify(conditionalTextRuleSetService).update(dto.getRuleSets(), conditionalText);
        verify(conditionalTextResultService).update(resultDto, conditionalText);
    }

    @Test
    public void testPut() {
        MessageConditionalText conditionalText = MessageConditionalText.builder().content(content).label("old").build();
        conditionalText.setUuid(UUID.randomUUID());
        MessageConditionalTextDto dto = MessageConditionalTextDto.builder().label("new").build();
        when(conditionalTextRepository.save(conditionalText)).thenReturn(conditionalText);
        when(conditionalTextRuleSetService.toDto(conditionalText.getRuleSets())).thenReturn(List.of());

        MessageConditionalTextDto result = conditionalTextService.put(dto, conditionalText);

        assertEquals(conditionalText.getUuid(), result.getId());
        assertEquals("new", result.getLabel());
        verify(conditionalTextRepository).save(conditionalText);
    }

    @Test
    public void testDuplicateList() {
        MessageConditionalText text1 = MessageConditionalText.builder().content(content).label("one").build();
        MessageConditionalText text2 = MessageConditionalText.builder().content(content).label("two").build();

        conditionalTextService.duplicate(List.of(text1, text2), content);

        assertEquals(2, content.getConditionalTexts().size());
    }

    @Test
    public void testDuplicateListNull() {
        conditionalTextService.duplicate((List<MessageConditionalText>) null, content);

        assertTrue(content.getConditionalTexts().isEmpty());
    }

    @Test
    public void testDuplicateSingle() {
        MessageConditionalTextResult result = MessageConditionalTextResult.builder().html("html").build();
        MessageConditionalText original = MessageConditionalText.builder()
            .content(content)
            .label("original")
            .result(result)
            .build();
        original.setUuid(UUID.randomUUID());

        conditionalTextService.duplicate(original, content);

        assertEquals(1, content.getConditionalTexts().size());
        MessageConditionalText duplicated = content.getConditionalTexts().get(0);
        assertEquals("original", duplicated.getLabel());
        assertSame(content, duplicated.getContent());
        assertTrue(!duplicated.getUuid().equals(original.getUuid()));
        verify(conditionalTextRuleSetService).duplicate(original.getRuleSets(), duplicated);
        verify(conditionalTextResultService).duplicate(result, duplicated);
    }

    @Test
    public void testDelete() {
        MessageConditionalText conditionalText = MessageConditionalText.builder().content(content).build();

        conditionalTextService.delete(conditionalText);

        verify(conditionalTextRepository).delete(conditionalText);
    }

    @Test
    public void testUpsertCreatesWhenIdNull() {
        MessageConditionalTextDto dto = MessageConditionalTextDto.builder().label("brand-new").build();

        conditionalTextService.upsert(List.of(dto), content);

        assertEquals(1, content.getConditionalTexts().size());
        verify(conditionalTextRepository, never()).findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(any(), any(), any());
    }

    @Test
    public void testUpsertUpdatesWhenFound() {
        MessageConditionalText existing = MessageConditionalText.builder().content(content).label("old").build();
        UUID existingId = UUID.randomUUID();
        existing.setUuid(existingId);
        MessageConditionalTextDto dto = MessageConditionalTextDto.builder().id(existingId).label("updated").build();
        when(conditionalTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(existingId, content.getUuid(), owner.getLmsUserId()))
            .thenReturn(Optional.of(existing));

        conditionalTextService.upsert(List.of(dto), content);

        assertEquals("updated", existing.getLabel());
        assertTrue(content.getConditionalTexts().isEmpty());
    }

    @Test
    public void testUpsertCreatesWhenIdNotFound() {
        UUID missingId = UUID.randomUUID();
        MessageConditionalTextDto dto = MessageConditionalTextDto.builder().id(missingId).label("new-from-missing").build();
        when(conditionalTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(missingId, content.getUuid(), owner.getLmsUserId()))
            .thenReturn(Optional.empty());

        conditionalTextService.upsert(List.of(dto), content);

        assertEquals(1, content.getConditionalTexts().size());
        assertEquals("new-from-missing", content.getConditionalTexts().get(0).getLabel());
    }

    @Test
    public void testToDtoList() {
        MessageConditionalText conditionalText = MessageConditionalText.builder().content(content).label("label").build();
        conditionalText.setUuid(UUID.randomUUID());
        when(conditionalTextRuleSetService.toDto(conditionalText.getRuleSets())).thenReturn(List.of());

        List<MessageConditionalTextDto> dtos = conditionalTextService.toDto(List.of(conditionalText));

        assertEquals(1, dtos.size());
        assertEquals("label", dtos.get(0).getLabel());
    }

    @Test
    public void testToDtoSingle() {
        MessageConditionalTextResult result = MessageConditionalTextResult.builder().html("html").build();
        MessageConditionalText conditionalText = MessageConditionalText.builder()
            .content(content)
            .label("label")
            .result(result)
            .build();
        conditionalText.setUuid(UUID.randomUUID());
        MessageConditionalTextResultDto resultDto = MessageConditionalTextResultDto.builder().html("html").build();
        List<MessageConditionalTextRuleSetDto> ruleSetDtos = List.of();
        when(conditionalTextResultService.toDto(result)).thenReturn(resultDto);
        when(conditionalTextRuleSetService.toDto(conditionalText.getRuleSets())).thenReturn(ruleSetDtos);

        MessageConditionalTextDto dto = conditionalTextService.toDto(conditionalText);

        assertEquals(conditionalText.getUuid(), dto.getId());
        assertEquals(content.getUuid(), dto.getContentId());
        assertEquals("label", dto.getLabel());
        assertSame(resultDto, dto.getResult());
        assertSame(ruleSetDtos, dto.getRuleSets());
    }

    @Test
    public void testFromDtoTwoArgDoesNotIncludeResultOrRuleSets() {
        MessageConditionalTextDto dto = MessageConditionalTextDto.builder()
            .label("label")
            .result(MessageConditionalTextResultDto.builder().html("html").build())
            .ruleSets(List.of(MessageConditionalTextRuleSetDto.builder().build()))
            .build();
        MessageConditionalText conditionalText = MessageConditionalText.builder().content(content).build();

        MessageConditionalText returned = conditionalTextService.fromDto(dto, conditionalText);

        assertEquals("label", returned.getLabel());
        verify(conditionalTextResultService, never()).fromDto(any(), any());
        verify(conditionalTextRuleSetService, never()).fromDto(any(List.class), any(), eq(true));
    }

    @Test
    public void testFromDtoNullDto() {
        MessageConditionalText conditionalText = MessageConditionalText.builder().content(content).label("unchanged").build();

        MessageConditionalText returned = conditionalTextService.fromDto(null, conditionalText, true, true);

        assertSame(conditionalText, returned);
        assertEquals("unchanged", returned.getLabel());
    }

    @Test
    public void testFromDtoIncludeResultAndRuleSets() {
        MessageConditionalTextResultDto resultDto = MessageConditionalTextResultDto.builder().html("html").build();
        MessageConditionalTextRuleSetDto ruleSetDto = MessageConditionalTextRuleSetDto.builder().build();
        MessageConditionalTextDto dto = MessageConditionalTextDto.builder()
            .label("label")
            .result(resultDto)
            .ruleSets(List.of(ruleSetDto))
            .build();
        MessageConditionalText conditionalText = MessageConditionalText.builder().content(content).build();
        MessageConditionalTextResult resultEntity = MessageConditionalTextResult.builder().html("html").build();
        when(conditionalTextResultService.fromDto(eq(resultDto), any(MessageConditionalTextResult.class))).thenReturn(resultEntity);
        when(conditionalTextRuleSetService.fromDto(eq(dto.getRuleSets()), eq(conditionalText), eq(true))).thenReturn(List.of());

        MessageConditionalText returned = conditionalTextService.fromDto(dto, conditionalText, true, true);

        assertEquals("label", returned.getLabel());
        assertSame(resultEntity, returned.getResult());
        assertTrue(returned.getRuleSets().isEmpty());
    }

    @Test
    public void testFromDtoExcludeResultAndRuleSets() {
        MessageConditionalTextDto dto = MessageConditionalTextDto.builder()
            .label("label")
            .result(MessageConditionalTextResultDto.builder().html("html").build())
            .ruleSets(List.of(MessageConditionalTextRuleSetDto.builder().build()))
            .build();
        MessageConditionalText conditionalText = MessageConditionalText.builder().content(content).build();

        conditionalTextService.fromDto(dto, conditionalText, false, false);

        verify(conditionalTextResultService, never()).fromDto(any(), any());
        verify(conditionalTextRuleSetService, never()).fromDto(any(List.class), any(), eq(true));
    }

    @Test
    public void testFromDtoListEmpty() {
        assertTrue(conditionalTextService.fromDto(null, content, true, true).isEmpty());
    }

    @Test
    public void testFromDtoList() {
        MessageConditionalTextDto dto = MessageConditionalTextDto.builder().label("label").build();

        List<MessageConditionalText> result = conditionalTextService.fromDto(List.of(dto), content, false, false);

        assertEquals(1, result.size());
        assertEquals("label", result.get(0).getLabel());
        assertSame(content, result.get(0).getContent());
    }

    @Test
    public void testDuplicateSingleUuidIsRandomized() {
        MessageConditionalText original = MessageConditionalText.builder().content(content).label("original").build();
        original.setUuid(UUID.randomUUID());

        conditionalTextService.duplicate(original, content);
        conditionalTextService.duplicate(original, content);

        assertEquals(2, content.getConditionalTexts().size());
        assertTrue(!content.getConditionalTexts().get(0).getUuid().equals(content.getConditionalTexts().get(1).getUuid()));
    }

}
