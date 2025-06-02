package edu.iu.terracotta.service.app.messaging.impl.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFile;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.messaging.attachment.MessageContentAttachment;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentAttachmentDto;
import edu.iu.terracotta.dao.repository.messaging.content.MessageContentAttachmentRepository;
import edu.iu.terracotta.service.app.messaging.MessageContentAttachmentService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LambdaCanBeMethodReference"})
public class MessageContentAttachmentServiceImpl implements MessageContentAttachmentService {

    @Autowired private MessageContentAttachmentRepository contentAttachmentRepository;
    @Autowired private ApiClient apiClient;

    @Override
    public List<MessageContentAttachmentDto> get(MessageContent content) {
        List<MessageContentAttachment> existingAttachments = contentAttachmentRepository.findAllByContent_Id(content.getId());
        List<MessageContentAttachment> lmsAttachments = new ArrayList<>();

        try {
            // retrieve files from LMS
            List<LmsFile> lmsFiles = apiClient.getFiles(content.getMessage().getOwner());
            List<String> existingAttachmentIds = existingAttachments.stream()
                .map(MessageContentAttachment::getLmsId)
                .toList();

            lmsAttachments = lmsFiles.stream()
                .map(
                    lmsFile ->
                        MessageContentAttachment.builder()
                            .content(content)
                            .displayName(lmsFile.getDisplayName())
                            .filename(lmsFile.getFilename())
                            .lmsId(lmsFile.getId())
                            .size(lmsFile.getSize())
                            .url(lmsFile.getUrl())
                            .build()
                )
                .filter(
                    // only add non-existing files
                    contentAttachment -> !existingAttachmentIds.contains(contentAttachment.getLmsId())
                )
                .collect(Collectors.toList());
        } catch (ApiException | TerracottaConnectorException e) {
            log.error("Error retrieving files from LMS for message content with ID: [{}].", content.getId(), e);
        }

        existingAttachments.addAll(lmsAttachments);

        return toDto(existingAttachments);
    }

    @Override
    public void update(List<MessageContentAttachmentDto> contentAttachmentDtos, MessageContent content) {
        content.getAttachments().clear();

        if (CollectionUtils.isEmpty(contentAttachmentDtos)) {
            return;
        }

        content.getAttachments().addAll(fromDto(contentAttachmentDtos, content));
    }

    @Override
    public void duplicate(List<MessageContentAttachment> contentAttachments, MessageContent content) {
        if (CollectionUtils.isEmpty(contentAttachments)) {
            return;
        }

        content.setAttachments(
            contentAttachments.stream()
                .map(
                    contentAttachment -> {
                        return MessageContentAttachment.builder()
                            .content(content)
                            .displayName(contentAttachment.getDisplayName())
                            .filename(contentAttachment.getFilename())
                            .lmsId(contentAttachment.getLmsId())
                            .size(contentAttachment.getSize())
                            .url(contentAttachment.getUrl())
                            .build();
                    }
                )
                .collect(Collectors.toList())
        );
    }

    @Override
    public List<MessageContentAttachmentDto> toDto(List<MessageContentAttachment> contentAttachments) {
        return CollectionUtils.emptyIfNull(contentAttachments).stream()
            .map(contentAttachment -> toDto(contentAttachment))
            .toList();
    }

    @Override
    public MessageContentAttachmentDto toDto(MessageContentAttachment contentAttachment) {
        return MessageContentAttachmentDto.builder()
            .displayName(contentAttachment.getDisplayName())
            .filename(contentAttachment.getFilename())
            .id(contentAttachment.getUuid())
            .lmsId(contentAttachment.getLmsId())
            .size(contentAttachment.getSize())
            .url(contentAttachment.getUrl())
            .build();
    }

    @Override
    public Optional<MessageContentAttachment> fromDto(MessageContentAttachmentDto contentAttachmentDto, MessageContent content) {
        if (contentAttachmentDto == null) {
            return Optional.empty();
        }

        return Optional.of(
            MessageContentAttachment.builder()
                .content(content)
                .displayName(contentAttachmentDto.getDisplayName())
                .filename(contentAttachmentDto.getFilename())
                .lmsId(contentAttachmentDto.getLmsId())
                .size(contentAttachmentDto.getSize())
                .url(contentAttachmentDto.getUrl())
                .build()
        );
    }

    @Override
    public List<MessageContentAttachment> fromDto(List<MessageContentAttachmentDto> contentAttachmentDtos, MessageContent content) {
        if (CollectionUtils.isEmpty(contentAttachmentDtos)) {
            return List.of();
        }
        return contentAttachmentDtos.stream()
            .map(contentAttachmentDto -> fromDto(contentAttachmentDto, content))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

}
