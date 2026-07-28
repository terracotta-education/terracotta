package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerRepository;

public class ComponentUtilsImplTest extends BaseTest {

    @InjectMocks private ComponentUtilsImpl componentUtils;

    @Mock private MessageContainerRepository messageContainerRepository;
    @Mock private MessageContainer messageContainer;

    private static final String LMS_USER_ID = "lms-user-1";
    private static final long EXPOSURE_ID = 1L;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(ltiUserEntity.getLmsUserId()).thenReturn(LMS_USER_ID);
    }

    @Test
    public void testCalculateNextOrderNoAssignmentsNoContainers() {
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeletedOrderByAssignmentOrderDesc(anyLong(), anyBoolean())).thenReturn(Collections.emptyList());
        when(messageContainerRepository.findAllByExposure_ExposureIdAndOwner_LmsUserIdOrderByConfiguration_ContainerOrderDesc(anyLong(), anyString())).thenReturn(Collections.emptyList());

        int nextOrder = componentUtils.calculateNextOrder(EXPOSURE_ID, ltiUserEntity);

        assertEquals(1, nextOrder);
    }

    @Test
    public void testCalculateNextOrderWithAssignmentsOnly() {
        when(assignment.getAssignmentOrder()).thenReturn(5);
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeletedOrderByAssignmentOrderDesc(anyLong(), anyBoolean())).thenReturn(List.of(assignment));
        when(messageContainerRepository.findAllByExposure_ExposureIdAndOwner_LmsUserIdOrderByConfiguration_ContainerOrderDesc(anyLong(), anyString())).thenReturn(Collections.emptyList());

        int nextOrder = componentUtils.calculateNextOrder(EXPOSURE_ID, ltiUserEntity);

        assertEquals(6, nextOrder);
    }

    @Test
    public void testCalculateNextOrderContainerOrderGreaterThanAssignments() {
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeletedOrderByAssignmentOrderDesc(anyLong(), anyBoolean())).thenReturn(Collections.emptyList());
        when(messageContainer.getOrder()).thenReturn(10);
        when(messageContainerRepository.findAllByExposure_ExposureIdAndOwner_LmsUserIdOrderByConfiguration_ContainerOrderDesc(anyLong(), anyString())).thenReturn(List.of(messageContainer));

        int nextOrder = componentUtils.calculateNextOrder(EXPOSURE_ID, ltiUserEntity);

        assertEquals(11, nextOrder);
    }

    @Test
    public void testCalculateNextOrderContainerOrderNotGreaterThanAssignments() {
        when(assignment.getAssignmentOrder()).thenReturn(5);
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeletedOrderByAssignmentOrderDesc(anyLong(), anyBoolean())).thenReturn(List.of(assignment));
        when(messageContainer.getOrder()).thenReturn(3);
        when(messageContainerRepository.findAllByExposure_ExposureIdAndOwner_LmsUserIdOrderByConfiguration_ContainerOrderDesc(anyLong(), anyString())).thenReturn(List.of(messageContainer));

        int nextOrder = componentUtils.calculateNextOrder(EXPOSURE_ID, ltiUserEntity);

        assertEquals(6, nextOrder);
    }

}
