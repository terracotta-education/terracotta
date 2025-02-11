package edu.iu.terracotta.service.app.integrations;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.model.enums.integrations.IntegrationLaunchParameter;
import edu.iu.terracotta.service.app.integrations.impl.IntegrationLaunchParameterServiceImpl;

public class IntegrationLaunchParameterServiceImplTest extends BaseTest {

    @InjectMocks private IntegrationLaunchParameterServiceImpl integrationLaunchParameterService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    void testBuildQueryString() {
        String ret = integrationLaunchParameterService.buildQueryString(submission, 1);

        assertNotNull(ret);
        assertTrue(
            EnumUtils.getEnumList(IntegrationLaunchParameter.class).stream()
                .allMatch(parameter -> StringUtils.contains(ret, parameter.key()))
        );
    }

    @Test
    void testBuildQueryStringAssignmentDueDateNotNull() {
        when(assignment.getDueDate()).thenReturn(Timestamp.from(Instant.now()));
        String ret = integrationLaunchParameterService.buildQueryString(submission, 1);

        assertNotNull(ret);
        assertTrue(
            EnumUtils.getEnumList(IntegrationLaunchParameter.class).stream()
                .allMatch(parameter -> StringUtils.contains(ret, parameter.key()))
        );
    }

    @Test
    void testBuildPreviewQueryString() {
        String ret = integrationLaunchParameterService.buildPreviewQueryString(integration);

        assertNotNull(ret);
        assertTrue(
            EnumUtils.getEnumList(IntegrationLaunchParameter.class).stream()
                .allMatch(parameter -> StringUtils.contains(ret, parameter.key()))
        );
    }

    @Test
    void testBuildPreviewQueryStringAssignmentDueDateNotNull() {
        when(assignment.getDueDate()).thenReturn(Timestamp.from(Instant.now()));
        String ret = integrationLaunchParameterService.buildPreviewQueryString(integration);

        assertNotNull(ret);
        assertTrue(
            EnumUtils.getEnumList(IntegrationLaunchParameter.class).stream()
                .allMatch(parameter -> StringUtils.contains(ret, parameter.key()))
        );
    }

}
