package edu.iu.terracotta.service.app.messaging.impl.piped;

import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextItemDto;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextItemService;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextItemValueService;
import io.jsonwebtoken.lang.Collections;

@Service
public class MessagePipedTextItemServiceImpl implements MessagePipedTextItemService {

    @Autowired private MessagePipedTextItemValueService pipedTextItemValueService;

    @Override
    public void create(List<MessagePipedTextItemDto> pipedTextItemDtos, MessagePipedText pipedText) {
        pipedText.getItems().clear();

        pipedText.getItems().addAll(
            CollectionUtils.emptyIfNull(pipedTextItemDtos).stream()
                .map(
                    pipedTextItemDto -> {
                        MessagePipedTextItem pipedTextItem = fromDto(
                            pipedTextItemDto,
                            MessagePipedTextItem.builder()
                                .pipedText(pipedText)
                                .build()
                        );

                        pipedTextItemValueService.create(
                            pipedTextItemDto.getValues(),
                            pipedTextItem
                        );

                        return pipedTextItem;
                    }
                )
                .toList()
        );
    }

    private void create(MessagePipedTextItemDto pipedTextItemDto, MessagePipedText pipedText) {
        if (pipedTextItemDto == null || pipedText == null) {
            return;
        }

        MessagePipedTextItem pipedTextItem = fromDto(
            pipedTextItemDto,
            MessagePipedTextItem.builder()
                .pipedText(pipedText)
                .build()
        );

        pipedText.getItems().add(pipedTextItem);

        pipedTextItemValueService.create(
            pipedTextItemDto.getValues(),
            pipedTextItem
        );
    }

    @Override
    public void update(MessagePipedTextItemDto pipedTextItemDto, MessagePipedTextItem pipedTextItem) {
        fromDto(pipedTextItemDto, pipedTextItem);
        pipedTextItemValueService.upsert(pipedTextItemDto.getValues(), pipedTextItem);
    }

    @Override
    public void upsert(List<MessagePipedTextItemDto> pipedTextItemDtos, MessagePipedText pipedText) {
        if (CollectionUtils.isEmpty(pipedTextItemDtos)) {
            pipedText.setItems(null);
            return;
        }

        if (CollectionUtils.isEmpty(pipedText.getItems())) {
            // no pipedText items; create all new
            create(pipedTextItemDtos, pipedText);
        }

        pipedTextItemDtos.forEach(
            pipedTextItemDto -> {
                if (pipedText.getItems().stream()
                    .noneMatch(item -> item.getUuid().equals(pipedTextItemDto.getId()))
                ) {
                    // item does not exist; create new
                    create(pipedTextItemDto, pipedText);
                } else {
                    update(
                        pipedTextItemDto,
                        pipedText.getItems().stream()
                            .filter(item -> item.getUuid().equals(pipedTextItemDto.getId()))
                            .findFirst()
                            .orElse(null)
                    );
                }
            }
        );
    }

    @Override
    public void duplicate(List<MessagePipedTextItem> pipedTextItems, MessagePipedText pipedText) {
        pipedText.getItems().clear();

        pipedText.getItems().addAll(
            CollectionUtils.emptyIfNull(pipedTextItems).stream()
                .map(
                    pipedTextItem -> {
                        MessagePipedTextItem newPipedTextItem = MessagePipedTextItem.builder()
                            .key(pipedTextItem.getKey())
                            .pipedText(pipedText)
                            .build();
                        newPipedTextItem.setUuid(UUID.randomUUID());

                        pipedTextItemValueService.duplicate(pipedTextItem.getValues(), newPipedTextItem);

                        return newPipedTextItem;
                    }

                )
                .toList()
        );
    }

    @Override
    public MessagePipedTextItem fromDto(MessagePipedTextItemDto pipedTextItemDto, MessagePipedTextItem pipedTextItem) {
        return fromDto(pipedTextItemDto, pipedTextItem, false);
    }

    @Override
    public MessagePipedTextItem fromDto(MessagePipedTextItemDto pipedTextItemDto, MessagePipedTextItem pipedTextItem, boolean includeItemValues) {
        if (pipedTextItemDto == null) {
            return pipedTextItem;
        }

        pipedTextItem.setUuid(pipedTextItemDto.getId());
        pipedTextItem.setKey(pipedTextItemDto.getKey());

        if (includeItemValues) {
            pipedTextItem.setValues(
                pipedTextItemValueService.fromDto(pipedTextItemDto.getValues(), pipedTextItem)
            );
        }

        return pipedTextItem;
    }

    @Override
    public List<MessagePipedTextItem> fromDto(List<MessagePipedTextItemDto> pipedTextItemDtos, MessagePipedText pipedText) {
        return fromDto(pipedTextItemDtos, pipedText, false);
    }

    @Override
    public List<MessagePipedTextItem> fromDto(List<MessagePipedTextItemDto> pipedTextItemDtos, MessagePipedText pipedText, boolean includeItemValues) {
        if (CollectionUtils.isEmpty(pipedTextItemDtos)) {
            return Collections.emptyList();
        }

        return pipedTextItemDtos.stream()
            .map(
                pipedTextItemDto ->
                    fromDto(
                        pipedTextItemDto,
                        MessagePipedTextItem.builder()
                            .pipedText(pipedText)
                            .build(),
                        includeItemValues
                    )
            )
            .toList();
    }

    @Override
    public MessagePipedTextItemDto toDto(MessagePipedTextItem pipedTextItem) {
        if (pipedTextItem == null) {
            return null;
        }

        return MessagePipedTextItemDto.builder()
            .id(pipedTextItem.getUuid())
            .key(pipedTextItem.getKey())
            .pipedTextId(pipedTextItem.getPipedText().getUuid())
            .values(
                pipedTextItemValueService.toDto(pipedTextItem.getValues())
            )
            .build();
    }

    @Override
    public List<MessagePipedTextItemDto> toDto(List<MessagePipedTextItem> pipedTextItems) {
        if (CollectionUtils.isEmpty(pipedTextItems)) {
            return Collections.emptyList();
        }

        return pipedTextItems.stream()
            .map(this::toDto)
            .toList();
    }

}
