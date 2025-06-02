package edu.iu.terracotta.service.app.messaging.impl.piped;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItemValue;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextItemValueDto;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextItemValueService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class MessagePipedTextItemValueServiceImpl implements MessagePipedTextItemValueService {

    @Autowired private LtiUserRepository ltiUserRepository;

    @Override
    public void create(MessagePipedTextItemValueDto pipedTextItemValueDto, MessagePipedTextItem pipedTextItem) {
        MessagePipedTextItemValue pipedTextItemValue = fromDto(
            pipedTextItemValueDto,
            MessagePipedTextItemValue.builder()
                .item(pipedTextItem)
                .build()
        );

        if (pipedTextItemValue == null) {
            return;
        }

        pipedTextItem.getValues().add(pipedTextItemValue);
    }

    @Override
    public void create(List<MessagePipedTextItemValueDto> pipedTextItemValueDtos, MessagePipedTextItem pipedTextItem) {
        pipedTextItem.getValues().clear();

        pipedTextItem.getValues().addAll(
            CollectionUtils.emptyIfNull(pipedTextItemValueDtos).stream()
                .map(
                    pipedTextItemValueDto -> fromDto(
                        pipedTextItemValueDto,
                        MessagePipedTextItemValue.builder()
                            .item(pipedTextItem)
                            .build()
                    )
                )
                .filter(Objects::nonNull)
                .toList()
        );
    }

    @Override
    public void update(MessagePipedTextItemValueDto pipedTextItemValueDto, MessagePipedTextItemValue pipedTextItem) {
        if (pipedTextItemValueDto == null || pipedTextItem == null) {
            return;
        }

        fromDto(pipedTextItemValueDto, pipedTextItem);
    }

    @Override
    public void update(List<MessagePipedTextItemValueDto> pipedTextItemValueDtos, MessagePipedTextItem pipedTextItem) {
        List<MessagePipedTextItemValue> existingValues = pipedTextItem.getValues().stream().toList();

        pipedTextItem.getValues().clear();

        pipedTextItem.getValues().addAll(
            CollectionUtils.emptyIfNull(pipedTextItemValueDtos).stream()
                .map(
                    pipedTextItemValueDto -> {
                        Optional<MessagePipedTextItemValue> existingValue = existingValues.stream()
                            .filter(existing -> existing.getUuid().equals(pipedTextItemValueDto.getId()))
                            .findFirst();

                        if (existingValue.isEmpty()) {
                            create(pipedTextItemValueDto, pipedTextItem);
                        } else {
                            update(pipedTextItemValueDto, existingValue.get());
                        }

                        return existingValue.get();
                    }
                )
                .toList()
        );


        pipedTextItem.getValues().addAll(
            CollectionUtils.emptyIfNull(pipedTextItemValueDtos).stream()
                .map(
                    pipedTextItemValueDto -> fromDto(
                        pipedTextItemValueDto,
                        MessagePipedTextItemValue.builder()
                            .item(pipedTextItem)
                            .build()
                    )
                )
                .filter(Objects::nonNull)
                .toList()
        );
    }

    @Override
    public void upsert(List<MessagePipedTextItemValueDto> pipedTextItemValueDtos, MessagePipedTextItem pipedTextItem) {
        if (CollectionUtils.isEmpty(pipedTextItemValueDtos)) {
            pipedTextItem.setValues(null);
            return;
        }

        if (CollectionUtils.isEmpty(pipedTextItem.getValues())) {
            // no pipedTextItem values; create all new
            create(pipedTextItemValueDtos, pipedTextItem);
        }

        pipedTextItemValueDtos.forEach(
            pipedTextItemValueDto -> {
                if (pipedTextItem.getValues().stream()
                    .noneMatch(item -> item.getUuid().equals(pipedTextItemValueDto.getId()))
                ) {
                    // item does not exist; create new
                    create(pipedTextItemValueDto, pipedTextItem);
                } else {
                    update(
                        pipedTextItemValueDto,
                        pipedTextItem.getValues().stream()
                            .filter(item -> item.getUuid().equals(pipedTextItemValueDto.getId()))
                            .findFirst()
                            .orElse(null)
                    );
                }
            }
        );
    }

    @Override
    public void duplicate(List<MessagePipedTextItemValue> pipedTextItemValues, MessagePipedTextItem pipedTextItem) {
        pipedTextItem.getValues().clear();

        pipedTextItem.getValues().addAll(
            CollectionUtils.emptyIfNull(pipedTextItemValues).stream()
                .map(
                    pipedTextItemValue -> MessagePipedTextItemValue.builder()
                        .item(pipedTextItem)
                        .user(pipedTextItemValue.getUser())
                        .value(pipedTextItemValue.getValue())
                        .build()
                )
                .filter(Objects::nonNull)
                .toList()
        );
    }

    @Override
    public MessagePipedTextItemValue fromDto(MessagePipedTextItemValueDto pipedTextItemValueDto, MessagePipedTextItemValue pipedTextItemValue) {
        if (pipedTextItemValueDto == null) {
            return null;
        }

        LtiUserEntity user = ltiUserRepository.findFirstByUserId(pipedTextItemValueDto.getUserId());

        if (user == null) {
            // user not found; log error
            log.error("User with LMS user ID: [%s] in message ID: [%s] not found.",
                    pipedTextItemValueDto.getUserId(),
                    pipedTextItemValue.getItem().getPipedText().getContent().getMessage().getId()
            );

            return null;
        }

        pipedTextItemValue.setUser(user);
        pipedTextItemValue.setUuid(pipedTextItemValueDto.getId());
        pipedTextItemValue.setValue(pipedTextItemValueDto.getValue());

        return pipedTextItemValue;
    }

    @Override
    public List<MessagePipedTextItemValue> fromDto(List<MessagePipedTextItemValueDto> pipedTextItemValueDtos, MessagePipedTextItem pipedTextItem) {
        return CollectionUtils.emptyIfNull(pipedTextItemValueDtos).stream()
            .map(
                pipedTextItemValueDto ->
                    fromDto(
                        pipedTextItemValueDto,
                        MessagePipedTextItemValue.builder()
                            .item(pipedTextItem)
                            .build()
                    )
            )
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public MessagePipedTextItemValueDto toDto(MessagePipedTextItemValue pipedTextItemValue) {
        if (pipedTextItemValue == null) {
            return null;
        }

        return MessagePipedTextItemValueDto.builder()
            .id(pipedTextItemValue.getUuid())
            .pipedTextItemId(pipedTextItemValue.getItem().getUuid())
            .userId(pipedTextItemValue.getUser().getUserId())
            .value(pipedTextItemValue.getValue())
            .build();
    }

    @Override
    public List<MessagePipedTextItemValueDto> toDto(List<MessagePipedTextItemValue> pipedTextItemValues) {
        if (CollectionUtils.isEmpty(pipedTextItemValues)) {
            return Collections.emptyList();
        }

        return pipedTextItemValues.stream()
            .map(this::toDto)
            .toList();
    }

}
