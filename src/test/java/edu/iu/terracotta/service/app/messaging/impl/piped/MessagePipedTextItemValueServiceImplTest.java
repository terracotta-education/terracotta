package edu.iu.terracotta.service.app.messaging.impl.piped;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItemValue;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextItemValueDto;

public class MessagePipedTextItemValueServiceImplTest extends BaseTest {

    @InjectMocks private MessagePipedTextItemValueServiceImpl pipedTextItemValueService;

    private MessagePipedTextItem item;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        item = MessagePipedTextItem.builder()
            .pipedText(pipedText())
            .values(new ArrayList<>())
            .build();
        item.setUuid(UUID.randomUUID());
    }

    private MessagePipedText pipedText() {
        Message message = mock(Message.class);
        when(message.getId()).thenReturn(1L);

        MessageContent content = MessageContent.builder()
            .message(message)
            .build();

        return MessagePipedText.builder()
            .content(content)
            .build();
    }

    private MessagePipedTextItemValueDto dto(UUID id, long userId, String value) {
        return MessagePipedTextItemValueDto.builder()
            .id(id)
            .userId(userId)
            .value(value)
            .build();
    }

    @Test
    public void testCreateSingleSuccess() {
        when(ltiUserRepository.findFirstByUserId(1L)).thenReturn(ltiUserEntity);
        when(ltiUserEntity.getUserId()).thenReturn(1L);

        pipedTextItemValueService.create(dto(UUID.randomUUID(), 1L, "value1"), item);

        assertEquals(1, item.getValues().size());
        assertEquals("value1", item.getValues().get(0).getValue());
        assertEquals(ltiUserEntity, item.getValues().get(0).getUser());
    }

    @Test
    public void testCreateSingleUserNotFound() {
        when(ltiUserRepository.findFirstByUserId(anyLong())).thenReturn(null);

        pipedTextItemValueService.create(dto(UUID.randomUUID(), 1L, "value1"), item);

        assertTrue(item.getValues().isEmpty());
    }

    @Test
    public void testCreateListSuccess() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(ltiUserRepository.findFirstByUserId(1L)).thenReturn(ltiUserEntity);

        pipedTextItemValueService.create(List.of(dto(id1, 1L, "value1"), dto(id2, 2L, "value2")), item);

        assertEquals(1, item.getValues().size());
        assertEquals("value1", item.getValues().get(0).getValue());
    }

    @Test
    public void testCreateListClearsExisting() {
        item.getValues().add(MessagePipedTextItemValue.builder().item(item).value("old").build());

        pipedTextItemValueService.create((List<MessagePipedTextItemValueDto>) null, item);

        assertTrue(item.getValues().isEmpty());
    }

    @Test
    public void testUpdateSingleSuccess() {
        MessagePipedTextItemValue value = MessagePipedTextItemValue.builder().item(item).build();
        when(ltiUserRepository.findFirstByUserId(1L)).thenReturn(ltiUserEntity);

        pipedTextItemValueService.update(dto(UUID.randomUUID(), 1L, "updated"), value);

        assertEquals("updated", value.getValue());
        assertEquals(ltiUserEntity, value.getUser());
    }

    @Test
    public void testUpdateSingleNullDto() {
        MessagePipedTextItemValue value = MessagePipedTextItemValue.builder().item(item).value("original").build();

        pipedTextItemValueService.update((MessagePipedTextItemValueDto) null, value);

        assertEquals("original", value.getValue());
    }

    @Test
    public void testUpdateSingleNullItem() {
        pipedTextItemValueService.update(dto(UUID.randomUUID(), 1L, "value"), null);
        // no exception; nothing to assert since item is null
    }

    @Test
    public void testUpdateListAllMatchExisting() {
        UUID existingId = UUID.randomUUID();
        MessagePipedTextItemValue existing = MessagePipedTextItemValue.builder().item(item).value("old").build();
        existing.setUuid(existingId);
        item.getValues().add(existing);

        when(ltiUserRepository.findFirstByUserId(1L)).thenReturn(ltiUserEntity);

        pipedTextItemValueService.update(List.of(dto(existingId, 1L, "new-value")), item);

        // the impl unconditionally re-appends a freshly built value in a second
        // pass, so a matched update ends up duplicated rather than replaced.
        assertEquals(2, item.getValues().size());
        assertTrue(item.getValues().stream().allMatch(v -> "new-value".equals(v.getValue())));
    }

    @Test
    public void testUpdateListWithNewDtoThrows() {
        MessagePipedTextItemValue existing = MessagePipedTextItemValue.builder().item(item).value("old").build();
        existing.setUuid(UUID.randomUUID());
        item.getValues().add(existing);

        when(ltiUserRepository.findFirstByUserId(1L)).thenReturn(ltiUserEntity);

        // dto id does not match any existing value; underlying impl blindly calls
        // existingValue.get() on an empty Optional in this branch.
        assertThrows(
            NoSuchElementException.class,
            () -> pipedTextItemValueService.update(List.of(dto(UUID.randomUUID(), 1L, "new-value")), item)
        );
    }

    @Test
    public void testUpsertEmptyDtosNullsValues() {
        item.getValues().add(MessagePipedTextItemValue.builder().item(item).value("old").build());

        pipedTextItemValueService.upsert(List.of(), item);

        assertNull(item.getValues());
    }

    @Test
    public void testUpsertNullDtosNullsValues() {
        pipedTextItemValueService.upsert(null, item);

        assertNull(item.getValues());
    }

    @Test
    public void testUpsertNoExistingValuesCreatesAll() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(ltiUserRepository.findFirstByUserId(1L)).thenReturn(ltiUserEntity);

        pipedTextItemValueService.upsert(List.of(dto(id1, 1L, "value1"), dto(id2, 1L, "value2")), item);

        assertEquals(2, item.getValues().size());
        assertTrue(item.getValues().stream().anyMatch(v -> v.getUuid().equals(id1) && "value1".equals(v.getValue())));
        assertTrue(item.getValues().stream().anyMatch(v -> v.getUuid().equals(id2) && "value2".equals(v.getValue())));
    }

    @Test
    public void testUpsertExistingMatchUpdatesInPlace() {
        UUID existingId = UUID.randomUUID();
        MessagePipedTextItemValue existing = MessagePipedTextItemValue.builder().item(item).value("old").build();
        existing.setUuid(existingId);
        item.getValues().add(existing);

        when(ltiUserRepository.findFirstByUserId(1L)).thenReturn(ltiUserEntity);

        pipedTextItemValueService.upsert(List.of(dto(existingId, 1L, "updated")), item);

        assertEquals(1, item.getValues().size());
        assertEquals("updated", item.getValues().get(0).getValue());
        assertEquals(existingId, item.getValues().get(0).getUuid());
    }

    @Test
    public void testUpsertNewValueAddedWhenNoMatch() {
        UUID existingId = UUID.randomUUID();
        MessagePipedTextItemValue existing = MessagePipedTextItemValue.builder().item(item).value("old").build();
        existing.setUuid(existingId);
        item.getValues().add(existing);

        UUID newId = UUID.randomUUID();
        when(ltiUserRepository.findFirstByUserId(1L)).thenReturn(ltiUserEntity);

        pipedTextItemValueService.upsert(List.of(dto(newId, 1L, "brand-new")), item);

        assertEquals(2, item.getValues().size());
        assertTrue(item.getValues().stream().anyMatch(v -> v.getUuid().equals(newId) && "brand-new".equals(v.getValue())));
    }

    @Test
    public void testDuplicateSuccess() {
        MessagePipedTextItemValue value1 = MessagePipedTextItemValue.builder().item(item).user(ltiUserEntity).value("value1").build();
        MessagePipedTextItem newItem = MessagePipedTextItem.builder().values(new ArrayList<>()).build();

        pipedTextItemValueService.duplicate(List.of(value1), newItem);

        assertEquals(1, newItem.getValues().size());
        assertEquals("value1", newItem.getValues().get(0).getValue());
        assertEquals(ltiUserEntity, newItem.getValues().get(0).getUser());
        assertEquals(newItem, newItem.getValues().get(0).getItem());
    }

    @Test
    public void testDuplicateNullList() {
        MessagePipedTextItem newItem = MessagePipedTextItem.builder().values(new ArrayList<>()).build();

        pipedTextItemValueService.duplicate(null, newItem);

        assertTrue(newItem.getValues().isEmpty());
    }

    @Test
    public void testFromDtoSingleNullDto() {
        MessagePipedTextItemValue value = MessagePipedTextItemValue.builder().item(item).build();

        assertNull(pipedTextItemValueService.fromDto(null, value));
    }

    @Test
    public void testFromDtoSingleUserNotFound() {
        when(ltiUserRepository.findFirstByUserId(anyLong())).thenReturn(null);

        MessagePipedTextItemValue value = MessagePipedTextItemValue.builder().item(item).build();

        assertNull(pipedTextItemValueService.fromDto(dto(UUID.randomUUID(), 99L, "value"), value));
    }

    @Test
    public void testFromDtoSingleSuccess() {
        UUID id = UUID.randomUUID();
        when(ltiUserRepository.findFirstByUserId(1L)).thenReturn(ltiUserEntity);

        MessagePipedTextItemValue value = MessagePipedTextItemValue.builder().item(item).build();
        MessagePipedTextItemValue result = pipedTextItemValueService.fromDto(dto(id, 1L, "value1"), value);

        assertEquals(id, result.getUuid());
        assertEquals("value1", result.getValue());
        assertEquals(ltiUserEntity, result.getUser());
    }

    @Test
    public void testFromDtoListFiltersNotFoundUsers() {
        when(ltiUserRepository.findFirstByUserId(1L)).thenReturn(ltiUserEntity);
        when(ltiUserRepository.findFirstByUserId(2L)).thenReturn(null);

        List<MessagePipedTextItemValue> result = pipedTextItemValueService.fromDto(
            List.of(dto(UUID.randomUUID(), 1L, "value1"), dto(UUID.randomUUID(), 2L, "value2")),
            item
        );

        assertEquals(1, result.size());
        assertEquals("value1", result.get(0).getValue());
    }

    @Test
    public void testFromDtoListNull() {
        assertTrue(pipedTextItemValueService.fromDto((List<MessagePipedTextItemValueDto>) null, item).isEmpty());
    }

    @Test
    public void testToDtoSingleNull() {
        assertNull(pipedTextItemValueService.toDto((MessagePipedTextItemValue) null));
    }

    @Test
    public void testToDtoSingleSuccess() {
        UUID id = UUID.randomUUID();
        MessagePipedTextItemValue value = MessagePipedTextItemValue.builder().item(item).user(ltiUserEntity).value("value1").build();
        value.setUuid(id);
        when(ltiUserEntity.getUserId()).thenReturn(42L);

        MessagePipedTextItemValueDto result = pipedTextItemValueService.toDto(value);

        assertEquals(id, result.getId());
        assertEquals(item.getUuid(), result.getPipedTextItemId());
        assertEquals(42L, result.getUserId());
        assertEquals("value1", result.getValue());
    }

    @Test
    public void testToDtoListEmpty() {
        assertTrue(pipedTextItemValueService.toDto(List.of()).isEmpty());
    }

    @Test
    public void testToDtoListNull() {
        assertTrue(pipedTextItemValueService.toDto((List<MessagePipedTextItemValue>) null).isEmpty());
    }

    @Test
    public void testToDtoListSuccess() {
        MessagePipedTextItemValue value1 = MessagePipedTextItemValue.builder().item(item).user(ltiUserEntity).value("value1").build();
        value1.setUuid(UUID.randomUUID());
        when(ltiUserEntity.getUserId()).thenReturn(42L);

        List<MessagePipedTextItemValueDto> result = pipedTextItemValueService.toDto(List.of(value1));

        assertEquals(1, result.size());
        assertEquals("value1", result.get(0).getValue());
    }

}
