package edu.iu.terracotta.service.app.messaging.impl.email;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerConfigurationDto;
import edu.iu.terracotta.dao.model.dto.messaging.email.MessageEmailReplyToDto;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageConfigurationDto;
import edu.iu.terracotta.service.app.messaging.MessageEmailReplyToService;

@Service
@SuppressWarnings({"PMD.LambdaCanBeMethodReference"})
public class MessageEmailReplyToServiceImpl implements MessageEmailReplyToService {

    @Override
    public void create(MessageContainerConfiguration containerConfiguration) {
        containerConfiguration.getReplyTo().add(
            MessageEmailReplyTo.builder()
                .email(containerConfiguration.getContainer().getOwner().getEmail())
                .containerConfiguration(containerConfiguration)
                .messageConfiguration(null)
                .build()
        );
    }

    @Override
    public void create(MessageConfiguration configuration) {
        configuration.getReplyTo().add(
            MessageEmailReplyTo.builder()
                .email(configuration.getMessage().getContainer().getOwner().getEmail())
                .containerConfiguration(null)
                .messageConfiguration(configuration)
                .build()
        );
    }

    @Override
    public void update(MessageContainerConfigurationDto containerConfigurationDto, MessageContainerConfiguration containerConfiguration) {
       // delete existing reply to addresses
        containerConfiguration.getReplyTo().clear();

        // create new reply to addresses
        containerConfiguration.getReplyTo().addAll(
            CollectionUtils.emptyIfNull(containerConfigurationDto.getReplyTo()).stream()
                .map(
                    replyToDto ->
                        MessageEmailReplyTo.builder()
                            .email(replyToDto.getEmail())
                            .containerConfiguration(containerConfiguration)
                            .messageConfiguration(null)
                            .build()
                )
                .collect(Collectors.toList()) // need modifiable list
        );
    }

    @Override
    public void update(MessageConfigurationDto configurationDto, MessageConfiguration configuration) {
        // delete existing reply to addresses
        configuration.getReplyTo().clear();

        if (CollectionUtils.isEmpty(configurationDto.getReplyTo())) {
            // no replyTo addresses exist; create new ones from container
            duplicate(
                configuration.getMessage().getContainer().getReplyTo(),
                configuration
            );

            return;
        }

        // create new reply to addresses
        configuration.getReplyTo().addAll(
            CollectionUtils.emptyIfNull(configurationDto.getReplyTo()).stream()
                .map(
                    replyToDto ->
                        MessageEmailReplyTo.builder()
                            .email(replyToDto.getEmail())
                            .containerConfiguration(null)
                            .messageConfiguration(configuration)
                            .build()
                )
                .collect(Collectors.toList()) // need modifiable list
        );
    }

    @Override
    public void duplicate(List<MessageEmailReplyTo> messageEmailReplyTos, MessageContainerConfiguration containerConfiguration) {
        containerConfiguration.getReplyTo().addAll(
            messageEmailReplyTos.stream()
                .map(
                    messageEmailReplyTo ->
                        MessageEmailReplyTo.builder()
                            .containerConfiguration(containerConfiguration)
                            .email(messageEmailReplyTo.getEmail())
                            .messageConfiguration(null)
                            .build()
                )
                .toList()
        );
    }

    @Override
    public void duplicate(List<MessageEmailReplyTo> messageEmailReplyTos, MessageConfiguration configuration) {
        configuration.getReplyTo().addAll(
            messageEmailReplyTos.stream()
                .map(
                    messageEmailReplyTo ->
                        MessageEmailReplyTo.builder()
                            .containerConfiguration(null)
                            .email(messageEmailReplyTo.getEmail())
                            .messageConfiguration(configuration)
                            .build()
                )
                .toList()
        );
    }

    @Override
    public List<MessageEmailReplyToDto> toDto(List<MessageEmailReplyTo> messageEmailReplyTos) {
        return CollectionUtils.emptyIfNull(messageEmailReplyTos).stream()
            .map(messageEmailReplyTo -> toDto(messageEmailReplyTo))
            .toList();
    }

    @Override
    public MessageEmailReplyToDto toDto(MessageEmailReplyTo messageEmailReplyTo) {
        if (messageEmailReplyTo == null) {
            return null;
        }

        return MessageEmailReplyToDto.builder()
            .containerConfigurationId(messageEmailReplyTo.getContainerConfiguration() != null ? messageEmailReplyTo.getContainerConfiguration().getUuid() : null)
            .email(messageEmailReplyTo.getEmail())
            .id(messageEmailReplyTo.getUuid())
            .messageConfigurationId(messageEmailReplyTo.getMessageConfiguration() != null ? messageEmailReplyTo.getMessageConfiguration().getUuid(): null)
            .build();
    }

}
