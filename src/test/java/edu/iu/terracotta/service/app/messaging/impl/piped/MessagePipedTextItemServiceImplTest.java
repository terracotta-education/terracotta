package edu.iu.terracotta.service.app.messaging.impl.piped;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItemValue;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextItemDto;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextItemValueDto;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextItemValueService;

@SuppressWarnings("unchecked")
public class MessagePipedTextItemServiceImplTest extends BaseTest {

    @Mock private MessagePipedTextItemValueService pipedTextItemValueService;

    @InjectMocks private MessagePipedTextItemServiceImpl pipedTextItemService;

    private MessagePipedText pipedText;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        pipedText = MessagePipedText.builder().items(new ArrayList<>()).build();
        pipedText.setUuid(UUID.randomUUID());
    }

    private MessagePipedTextItemDto dto(UUID id, String key) {
        return MessagePipedTextItemDto.builder()
            .id(id)
            .key(key)
            .values(List.of())
            .build();
    }

    @Test
    public void testCreateListSuccess() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        pipedTextItemService.create(List.of(dto(id1, "key1"), dto(id2, "key2")), pipedText);

        assertEquals(2, pipedText.getItems().size());
        assertTrue(pipedText.getItems().stream().anyMatch(i -> i.getUuid().equals(id1) && "key1".equals(i.getKey())));
        assertTrue(pipedText.getItems().stream().anyMatch(i -> i.getUuid().equals(id2) && "key2".equals(i.getKey())));
        verify(pipedTextItemValueService, times(2)).create(any(List.class), any(MessagePipedTextItem.class));
    }

    @Test
    public void testCreateListNull() {
        pipedText.getItems().add(MessagePipedTextItem.builder().pipedText(pipedText).build());

        pipedTextItemService.create((List<MessagePipedTextItemDto>) null, pipedText);

        assertTrue(pipedText.getItems().isEmpty());
    }

    @Test
    public void testCreateListClearsExistingItems() {
        pipedText.getItems().add(MessagePipedTextItem.builder().pipedText(pipedText).key("stale").build());
        UUID id1 = UUID.randomUUID();

        pipedTextItemService.create(List.of(dto(id1, "fresh")), pipedText);

        assertEquals(1, pipedText.getItems().size());
        assertEquals("fresh", pipedText.getItems().get(0).getKey());
    }

    @Test
    public void testUpdateSuccess() {
        MessagePipedTextItem item = MessagePipedTextItem.builder().pipedText(pipedText).build();
        UUID id = UUID.randomUUID();

        pipedTextItemService.update(dto(id, "updated-key"), item);

        assertEquals(id, item.getUuid());
        assertEquals("updated-key", item.getKey());
        verify(pipedTextItemValueService).upsert(any(List.class), any(MessagePipedTextItem.class));
    }

    @Test
    public void testUpsertEmptyDtosNullsItems() {
        pipedText.getItems().add(MessagePipedTextItem.builder().pipedText(pipedText).build());

        pipedTextItemService.upsert(List.of(), pipedText);

        assertNull(pipedText.getItems());
    }

    @Test
    public void testUpsertNullDtosNullsItems() {
        pipedTextItemService.upsert(null, pipedText);

        assertNull(pipedText.getItems());
    }

    @Test
    public void testUpsertNoExistingItemsCreatesAll() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        pipedTextItemService.upsert(List.of(dto(id1, "key1"), dto(id2, "key2")), pipedText);

        assertEquals(2, pipedText.getItems().size());
        assertTrue(pipedText.getItems().stream().anyMatch(i -> i.getUuid().equals(id1)));
        assertTrue(pipedText.getItems().stream().anyMatch(i -> i.getUuid().equals(id2)));
    }

    @Test
    public void testUpsertExistingItemMatchUpdatesInPlace() {
        UUID existingId = UUID.randomUUID();
        MessagePipedTextItem existing = MessagePipedTextItem.builder().pipedText(pipedText).key("old").build();
        existing.setUuid(existingId);
        pipedText.getItems().add(existing);

        pipedTextItemService.upsert(List.of(dto(existingId, "new-key")), pipedText);

        assertEquals(1, pipedText.getItems().size());
        assertEquals("new-key", pipedText.getItems().get(0).getKey());
        assertEquals(existingId, pipedText.getItems().get(0).getUuid());
        verify(pipedTextItemValueService).upsert(any(List.class), any(MessagePipedTextItem.class));
    }

    @Test
    public void testUpsertNewItemAddedWhenNoMatch() {
        UUID existingId = UUID.randomUUID();
        MessagePipedTextItem existing = MessagePipedTextItem.builder().pipedText(pipedText).key("old").build();
        existing.setUuid(existingId);
        pipedText.getItems().add(existing);

        UUID newId = UUID.randomUUID();

        pipedTextItemService.upsert(List.of(dto(newId, "new-key")), pipedText);

        assertEquals(2, pipedText.getItems().size());
        assertTrue(pipedText.getItems().stream().anyMatch(i -> i.getUuid().equals(newId) && "new-key".equals(i.getKey())));
    }

    @Test
    public void testDuplicateSuccess() {
        MessagePipedTextItem source = MessagePipedTextItem.builder().key("key1").values(new ArrayList<>()).build();
        source.getValues().add(MessagePipedTextItemValue.builder().item(source).value("value1").build());

        pipedTextItemService.duplicate(List.of(source), pipedText);

        assertEquals(1, pipedText.getItems().size());
        MessagePipedTextItem duplicated = pipedText.getItems().get(0);
        assertEquals("key1", duplicated.getKey());
        assertEquals(pipedText, duplicated.getPipedText());
        assertTrue(duplicated.getUuid() != null);
        verify(pipedTextItemValueService).duplicate(any(List.class), any(MessagePipedTextItem.class));
    }

    @Test
    public void testDuplicateNullListClearsItems() {
        pipedText.getItems().add(MessagePipedTextItem.builder().pipedText(pipedText).build());

        pipedTextItemService.duplicate(null, pipedText);

        assertTrue(pipedText.getItems().isEmpty());
    }

    @Test
    public void testFromDtoSingleTwoArgDelegatesWithoutValues() {
        MessagePipedTextItem item = MessagePipedTextItem.builder().pipedText(pipedText).build();
        UUID id = UUID.randomUUID();

        MessagePipedTextItem result = pipedTextItemService.fromDto(dto(id, "key1"), item);

        assertEquals(id, result.getUuid());
        assertEquals("key1", result.getKey());
        verify(pipedTextItemValueService, never()).fromDto(any(List.class), any(MessagePipedTextItem.class));
    }

    @Test
    public void testFromDtoSingleNullDto() {
        MessagePipedTextItem item = MessagePipedTextItem.builder().pipedText(pipedText).build();

        MessagePipedTextItem result = pipedTextItemService.fromDto((MessagePipedTextItemDto) null, item, true);

        assertEquals(item, result);
    }

    @Test
    public void testFromDtoSingleIncludeValuesTrue() {
        MessagePipedTextItem item = MessagePipedTextItem.builder().pipedText(pipedText).build();
        List<MessagePipedTextItemValue> values = List.of(MessagePipedTextItemValue.builder().item(item).value("v1").build());
        when(pipedTextItemValueService.fromDto(any(List.class), any(MessagePipedTextItem.class))).thenReturn(values);

        MessagePipedTextItem result = pipedTextItemService.fromDto(dto(UUID.randomUUID(), "key1"), item, true);

        assertEquals(values, result.getValues());
    }

    @Test
    public void testFromDtoListTwoArgDelegatesWithoutValues() {
        UUID id = UUID.randomUUID();

        List<MessagePipedTextItem> result = pipedTextItemService.fromDto(List.of(dto(id, "key1")), pipedText);

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getUuid());
        verify(pipedTextItemValueService, never()).fromDto(any(List.class), any(MessagePipedTextItem.class));
    }

    @Test
    public void testFromDtoListEmpty() {
        assertTrue(pipedTextItemService.fromDto(List.of(), pipedText, false).isEmpty());
    }

    @Test
    public void testFromDtoListNull() {
        assertTrue(pipedTextItemService.fromDto((List<MessagePipedTextItemDto>) null, pipedText, false).isEmpty());
    }

    @Test
    public void testFromDtoListIncludeValuesTrue() {
        UUID id = UUID.randomUUID();
        List<MessagePipedTextItemValue> values = List.of();
        when(pipedTextItemValueService.fromDto(any(List.class), any(MessagePipedTextItem.class))).thenReturn(values);

        List<MessagePipedTextItem> result = pipedTextItemService.fromDto(List.of(dto(id, "key1")), pipedText, true);

        assertEquals(1, result.size());
        assertEquals(pipedText, result.get(0).getPipedText());
        assertEquals(values, result.get(0).getValues());
    }

    @Test
    public void testToDtoSingleNull() {
        assertNull(pipedTextItemService.toDto((MessagePipedTextItem) null));
    }

    @Test
    public void testToDtoSingleSuccess() {
        MessagePipedTextItem item = MessagePipedTextItem.builder().pipedText(pipedText).key("key1").build();
        UUID id = UUID.randomUUID();
        item.setUuid(id);
        List<MessagePipedTextItemValueDto> valueDtos = List.of(MessagePipedTextItemValueDto.builder().value("v1").build());
        when(pipedTextItemValueService.toDto(item.getValues())).thenReturn(valueDtos);

        MessagePipedTextItemDto result = pipedTextItemService.toDto(item);

        assertEquals(id, result.getId());
        assertEquals("key1", result.getKey());
        assertEquals(pipedText.getUuid(), result.getPipedTextId());
        assertEquals(valueDtos, result.getValues());
    }

    @Test
    public void testToDtoListEmpty() {
        assertTrue(pipedTextItemService.toDto(List.of()).isEmpty());
    }

    @Test
    public void testToDtoListNull() {
        assertTrue(pipedTextItemService.toDto((List<MessagePipedTextItem>) null).isEmpty());
    }

    @Test
    public void testToDtoListSuccess() {
        MessagePipedTextItem item = MessagePipedTextItem.builder().pipedText(pipedText).key("key1").build();
        item.setUuid(UUID.randomUUID());

        List<MessagePipedTextItemDto> result = pipedTextItemService.toDto(List.of(item));

        assertEquals(1, result.size());
        assertEquals("key1", result.get(0).getKey());
    }

}
