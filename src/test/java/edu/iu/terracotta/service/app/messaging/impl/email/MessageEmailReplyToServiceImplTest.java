package edu.iu.terracotta.service.app.messaging.impl.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerConfigurationDto;
import edu.iu.terracotta.dao.model.dto.messaging.email.MessageEmailReplyToDto;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageConfigurationDto;

public class MessageEmailReplyToServiceImplTest extends BaseTest {

    @InjectMocks private MessageEmailReplyToServiceImpl messageEmailReplyToService;

    private LtiUserEntity owner;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        owner = LtiUserEntity.builder()
            .email("owner@terracotta.edu")
            .build();
    }

    @Test
    public void testCreateContainerConfiguration() {
        MessageContainer container = MessageContainer.builder().owner(owner).build();
        MessageContainerConfiguration containerConfiguration = MessageContainerConfiguration.builder().container(container).build();

        messageEmailReplyToService.create(containerConfiguration);

        assertEquals(1, containerConfiguration.getReplyTo().size());
        MessageEmailReplyTo replyTo = containerConfiguration.getReplyTo().get(0);
        assertEquals("owner@terracotta.edu", replyTo.getEmail());
        assertEquals(containerConfiguration, replyTo.getContainerConfiguration());
        assertNull(replyTo.getMessageConfiguration());
    }

    @Test
    public void testCreateMessageConfiguration() {
        MessageContainer container = MessageContainer.builder().owner(owner).build();
        Message message = Message.builder().container(container).build();
        MessageConfiguration configuration = MessageConfiguration.builder().message(message).build();

        messageEmailReplyToService.create(configuration);

        assertEquals(1, configuration.getReplyTo().size());
        MessageEmailReplyTo replyTo = configuration.getReplyTo().get(0);
        assertEquals("owner@terracotta.edu", replyTo.getEmail());
        assertEquals(configuration, replyTo.getMessageConfiguration());
        assertNull(replyTo.getContainerConfiguration());
    }

    @Test
    public void testUpdateContainerConfigurationReplacesExistingList() {
        MessageContainerConfiguration containerConfiguration = MessageContainerConfiguration.builder().build();
        containerConfiguration.getReplyTo().add(MessageEmailReplyTo.builder().email("old@terracotta.edu").build());
        MessageContainerConfigurationDto dto = MessageContainerConfigurationDto.builder()
            .replyTo(
                List.of(
                    MessageEmailReplyToDto.builder().email("new1@terracotta.edu").build(),
                    MessageEmailReplyToDto.builder().email("new2@terracotta.edu").build()
                )
            )
            .build();

        messageEmailReplyToService.update(dto, containerConfiguration);

        assertEquals(2, containerConfiguration.getReplyTo().size());
        assertTrue(containerConfiguration.getReplyTo().stream().anyMatch(r -> "new1@terracotta.edu".equals(r.getEmail())));
        assertTrue(containerConfiguration.getReplyTo().stream().anyMatch(r -> "new2@terracotta.edu".equals(r.getEmail())));
        assertTrue(containerConfiguration.getReplyTo().stream().allMatch(r -> r.getContainerConfiguration() == containerConfiguration));
        assertTrue(containerConfiguration.getReplyTo().stream().allMatch(r -> r.getMessageConfiguration() == null));
    }

    @Test
    public void testUpdateContainerConfigurationWithNullDtoListClearsExisting() {
        MessageContainerConfiguration containerConfiguration = MessageContainerConfiguration.builder().build();
        containerConfiguration.getReplyTo().add(MessageEmailReplyTo.builder().email("old@terracotta.edu").build());
        MessageContainerConfigurationDto dto = MessageContainerConfigurationDto.builder().replyTo(null).build();

        messageEmailReplyToService.update(dto, containerConfiguration);

        assertTrue(containerConfiguration.getReplyTo().isEmpty());
    }

    @Test
    public void testUpdateMessageConfigurationWithReplyToUsesDtoValues() {
        MessageConfiguration configuration = MessageConfiguration.builder().build();
        configuration.getReplyTo().add(MessageEmailReplyTo.builder().email("old@terracotta.edu").build());
        MessageConfigurationDto dto = MessageConfigurationDto.builder()
            .replyTo(List.of(MessageEmailReplyToDto.builder().email("new@terracotta.edu").build()))
            .build();

        messageEmailReplyToService.update(dto, configuration);

        assertEquals(1, configuration.getReplyTo().size());
        assertEquals("new@terracotta.edu", configuration.getReplyTo().get(0).getEmail());
        assertEquals(configuration, configuration.getReplyTo().get(0).getMessageConfiguration());
    }

    @Test
    public void testUpdateMessageConfigurationWithEmptyDtoListDuplicatesFromContainer() {
        MessageContainerConfiguration containerConfiguration = MessageContainerConfiguration.builder().build();
        containerConfiguration.getReplyTo().add(MessageEmailReplyTo.builder().email("container@terracotta.edu").build());
        MessageContainer container = MessageContainer.builder().configuration(containerConfiguration).build();
        Message message = Message.builder().container(container).build();
        MessageConfiguration configuration = MessageConfiguration.builder().message(message).build();
        MessageConfigurationDto dto = MessageConfigurationDto.builder().replyTo(List.of()).build();

        messageEmailReplyToService.update(dto, configuration);

        assertEquals(1, configuration.getReplyTo().size());
        assertEquals("container@terracotta.edu", configuration.getReplyTo().get(0).getEmail());
        assertEquals(configuration, configuration.getReplyTo().get(0).getMessageConfiguration());
        assertNull(configuration.getReplyTo().get(0).getContainerConfiguration());
    }

    @Test
    public void testDuplicateToContainerConfiguration() {
        List<MessageEmailReplyTo> source = List.of(MessageEmailReplyTo.builder().email("a@terracotta.edu").build());
        MessageContainerConfiguration target = MessageContainerConfiguration.builder().build();

        messageEmailReplyToService.duplicate(source, target);

        assertEquals(1, target.getReplyTo().size());
        assertEquals("a@terracotta.edu", target.getReplyTo().get(0).getEmail());
        assertEquals(target, target.getReplyTo().get(0).getContainerConfiguration());
        assertNull(target.getReplyTo().get(0).getMessageConfiguration());
    }

    @Test
    public void testDuplicateToMessageConfiguration() {
        List<MessageEmailReplyTo> source = List.of(MessageEmailReplyTo.builder().email("b@terracotta.edu").build());
        MessageConfiguration target = MessageConfiguration.builder().build();

        messageEmailReplyToService.duplicate(source, target);

        assertEquals(1, target.getReplyTo().size());
        assertEquals("b@terracotta.edu", target.getReplyTo().get(0).getEmail());
        assertEquals(target, target.getReplyTo().get(0).getMessageConfiguration());
        assertNull(target.getReplyTo().get(0).getContainerConfiguration());
    }

    @Test
    public void testToDtoSingleNullReturnsNull() {
        assertNull(messageEmailReplyToService.toDto((MessageEmailReplyTo) null));
    }

    @Test
    public void testToDtoSingleWithContainerConfiguration() {
        MessageContainerConfiguration containerConfiguration = MessageContainerConfiguration.builder().build();
        containerConfiguration.setUuid(UUID.randomUUID());
        MessageEmailReplyTo replyTo = MessageEmailReplyTo.builder()
            .email("c@terracotta.edu")
            .containerConfiguration(containerConfiguration)
            .build();
        replyTo.setUuid(UUID.randomUUID());

        MessageEmailReplyToDto dto = messageEmailReplyToService.toDto(replyTo);

        assertEquals(replyTo.getUuid(), dto.getId());
        assertEquals("c@terracotta.edu", dto.getEmail());
        assertEquals(containerConfiguration.getUuid(), dto.getContainerConfigurationId());
        assertNull(dto.getMessageConfigurationId());
    }

    @Test
    public void testToDtoSingleWithMessageConfiguration() {
        MessageConfiguration configuration = MessageConfiguration.builder().build();
        configuration.setUuid(UUID.randomUUID());
        MessageEmailReplyTo replyTo = MessageEmailReplyTo.builder()
            .email("d@terracotta.edu")
            .messageConfiguration(configuration)
            .build();
        replyTo.setUuid(UUID.randomUUID());

        MessageEmailReplyToDto dto = messageEmailReplyToService.toDto(replyTo);

        assertEquals(configuration.getUuid(), dto.getMessageConfigurationId());
        assertNull(dto.getContainerConfigurationId());
    }

    @Test
    public void testToDtoListReturnsMappedEntries() {
        List<MessageEmailReplyTo> replyTos = List.of(
            MessageEmailReplyTo.builder().email("e1@terracotta.edu").build(),
            MessageEmailReplyTo.builder().email("e2@terracotta.edu").build()
        );

        List<MessageEmailReplyToDto> dtos = messageEmailReplyToService.toDto(replyTos);

        assertEquals(2, dtos.size());
        assertTrue(dtos.stream().anyMatch(d -> "e1@terracotta.edu".equals(d.getEmail())));
        assertTrue(dtos.stream().anyMatch(d -> "e2@terracotta.edu".equals(d.getEmail())));
    }

    @Test
    public void testToDtoListWithNullListReturnsEmptyList() {
        List<MessageEmailReplyToDto> dtos = messageEmailReplyToService.toDto((List<MessageEmailReplyTo>) null);

        assertTrue(dtos.isEmpty());
    }

}
