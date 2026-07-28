package edu.iu.terracotta.service.app.messaging.impl.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerConfigurationDto;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerRepository;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.service.app.messaging.MessageConfigurationService;
import edu.iu.terracotta.service.app.messaging.MessageContainerConfigurationService;
import edu.iu.terracotta.service.app.messaging.MessageEmailReplyToService;
import edu.iu.terracotta.service.app.messaging.MessageService;

public class MessageContainerServiceImplTest extends BaseTest {

    // exposureGroupConditionRepository and ltiUserRepository are inherited (protected) from
    // BaseRepositoryTest; redeclaring them here would shadow-by-name/type and make @InjectMocks'
    // field matching ambiguous between the two same-typed fields, non-deterministically wiring the
    // wrong (unstubbed, base-default) mock into messageContainerService.
    @Mock private MessageContainerRepository containerRepository;
    @Mock private MessageConfigurationService configurationService;
    @Mock private MessageContainerConfigurationService containerConfigurationService;
    @Mock private MessageEmailReplyToService messageEmailReplyToService;
    @Mock private MessageService messageService;

    @InjectMocks private MessageContainerServiceImpl messageContainerService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(ltiUserEntity.getLmsUserId()).thenReturn("lms-user-1");
        when(ltiUserEntity.getUserId()).thenReturn(1L);
        when(platformDeployment.getBaseUrl()).thenReturn("http://base.url");
        when(containerConfigurationService.toDto(any())).thenReturn(mock(MessageContainerConfigurationDto.class));
        when(messageService.toDto(anyList())).thenReturn(List.of());
    }

    private MessageContainer buildContainer(MessageStatus status) {
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .status(status)
            .build();
        MessageContainer container = MessageContainer.builder()
            .exposure(exposure)
            .owner(ltiUserEntity)
            .configuration(configuration)
            .messages(new ArrayList<>())
            .build();
        configuration.setContainer(container);

        return container;
    }

    @Test
    public void testGetAll() {
        MessageContainer container = buildContainer(MessageStatus.UNPUBLISHED);

        when(
            containerRepository.findAllByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOwner_LmsUserIdAndConfiguration_StatusInOrderByConfiguration_ContainerOrderAsc(
                eq(1L), eq(2L), eq("1"), anyList()
            )
        ).thenReturn(List.of(container));

        List<MessageContainerDto> result = messageContainerService.getAll(1L, 2L, securedInfo);

        assertEquals(1, result.size());
    }

    @Test
    public void testCreate() {
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);
        when(containerRepository.save(any(MessageContainer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageContainerDto result = messageContainerService.create(exposure, true, securedInfo);

        assertNotNull(result);
        verify(containerConfigurationService).create(any(MessageContainer.class), eq(ltiUserEntity));
        verify(messageService).create(any(MessageContainer.class), eq(1L), eq(true));
        verify(containerRepository).save(any(MessageContainer.class));
    }

    @Test
    public void testUpdate() {
        MessageContainer container = buildContainer(MessageStatus.UNPUBLISHED);
        MessageContainerConfigurationDto configurationDto = mock(MessageContainerConfigurationDto.class);
        MessageContainerDto containerDto = MessageContainerDto.builder().configuration(configurationDto).build();

        when(containerRepository.save(container)).thenReturn(container);

        MessageContainerDto result = messageContainerService.update(containerDto, container);

        assertNotNull(result);
        verify(containerConfigurationService).update(configurationDto, container.getConfiguration());
        verify(configurationService).propogateContainerChanges(container);
        verify(messageEmailReplyToService).update(configurationDto, container.getConfiguration());
        verify(containerRepository).save(container);
    }

    @Test
    public void testUpdateAllFiltersOutMissingContainers() {
        MessageContainer container = buildContainer(MessageStatus.UNPUBLISHED);
        UUID knownUuid = UUID.randomUUID();
        container.setUuid(knownUuid);
        MessageContainerDto matchingDto = MessageContainerDto.builder().id(knownUuid).configuration(mock(MessageContainerConfigurationDto.class)).build();
        MessageContainerDto unknownDto = MessageContainerDto.builder().id(UUID.randomUUID()).configuration(mock(MessageContainerConfigurationDto.class)).build();

        when(containerRepository.save(any(MessageContainer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<MessageContainerDto> results = messageContainerService.updateAll(List.of(matchingDto, unknownDto), List.of(container));

        assertEquals(1, results.size());
    }

    @Test
    public void testDelete() {
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);

        when(containerRepository.save(container)).thenReturn(container);

        MessageContainerDto result = messageContainerService.delete(container);

        assertNotNull(result);
        assertEquals(MessageStatus.DELETED, container.getConfiguration().getStatus());
        verify(messageService).delete(container);
        verify(containerRepository).save(container);
    }

    @Test
    public void testMoveSuccessfullyReassignsMessages() {
        Message message = mock(Message.class);
        when(message.getConditionId()).thenReturn(5L);
        MessageContainer container = buildContainer(MessageStatus.UNPUBLISHED);
        container.setMessages(new ArrayList<>(List.of(message)));

        Condition matchingCondition = mock(Condition.class);
        when(matchingCondition.getConditionId()).thenReturn(5L);
        ExposureGroupCondition newExposureGroupCondition = mock(ExposureGroupCondition.class);
        when(newExposureGroupCondition.getCondition()).thenReturn(matchingCondition);

        Exposure toExposure = mock(Exposure.class);
        when(toExposure.getExposureId()).thenReturn(99L);
        when(exposureGroupConditionRepository.findByExposure_ExposureId(anyLong())).thenReturn(List.of(newExposureGroupCondition));
        when(containerRepository.save(container)).thenReturn(container);

        MessageContainerDto result = messageContainerService.move(toExposure, container);

        assertNotNull(result);
        assertEquals(toExposure, container.getExposure());
        assertEquals(1, container.getMessages().size());
        verify(message).setExposureGroupCondition(newExposureGroupCondition);
    }

    @Test
    public void testMoveDropsMessagesWithNoMatchingCondition() {
        Message message = mock(Message.class);
        when(message.getConditionId()).thenReturn(99L);
        MessageContainer container = buildContainer(MessageStatus.UNPUBLISHED);
        container.setMessages(new ArrayList<>(List.of(message)));

        Exposure toExposure = mock(Exposure.class);
        when(toExposure.getExposureId()).thenReturn(99L);
        when(exposureGroupConditionRepository.findByExposure_ExposureId(anyLong())).thenReturn(Collections.emptyList());
        when(containerRepository.save(container)).thenReturn(container);

        MessageContainerDto result = messageContainerService.move(toExposure, container);

        assertNotNull(result);
        assertTrue(container.getMessages().isEmpty());
    }

    @Test
    public void testDuplicate() throws MessageBodyParseException {
        Message message = mock(Message.class);
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        container.setMessages(List.of(message));

        Exposure toExposure = mock(Exposure.class);
        when(containerRepository.save(any(MessageContainer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageContainerDto result = messageContainerService.duplicate(container, toExposure);

        assertNotNull(result);
        verify(containerConfigurationService).duplicate(eq(container.getConfiguration()), any(MessageContainer.class));
        verify(messageService).duplicate(eq(List.of(message)), any(MessageContainer.class));
    }

    @Test
    public void testDuplicatePropagatesMessageBodyParseException() throws MessageBodyParseException {
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        container.setMessages(List.of());
        Exposure toExposure = mock(Exposure.class);

        doThrow(new MessageBodyParseException("bad body")).when(messageService).duplicate(anyList(), any(MessageContainer.class));

        assertThrows(MessageBodyParseException.class, () -> messageContainerService.duplicate(container, toExposure));
    }

    @Test
    public void testToDtoListEmptyWhenNull() {
        assertTrue(messageContainerService.toDto((List<MessageContainer>) null).isEmpty());
    }

    @Test
    public void testToDtoListFiltersNonDisplayableContainers() {
        MessageContainer displayable = buildContainer(MessageStatus.UNPUBLISHED);
        MessageContainer deleted = buildContainer(MessageStatus.DELETED);

        List<MessageContainerDto> result = messageContainerService.toDto(List.of(displayable, deleted));

        assertEquals(1, result.size());
    }

    @Test
    public void testToDtoSingleNullReturnsNull() {
        assertNull(messageContainerService.toDto((MessageContainer) null));
    }

    @Test
    public void testToDtoSingleBuildsMyFilesUrl() {
        MessageContainer container = buildContainer(MessageStatus.UNPUBLISHED);

        MessageContainerDto result = messageContainerService.toDto(container);

        assertEquals(String.format(MessageContainerDto.MY_FILES_URL, "http://base.url", "lms-user-1"), result.getMyFilesUrl());
        assertEquals("abc@terracotta.edu", result.getOwnerEmail());
        assertEquals(1L, result.getOwnerId());
    }

}
