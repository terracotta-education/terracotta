package edu.iu.terracotta.service.app.integrations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.service.app.integrations.impl.IntegrationLaunchServiceImpl;

public class IntegrationLaunchServiceImplTest extends BaseTest {

    @InjectMocks private IntegrationLaunchServiceImpl integrationLaunchService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        clearInvocations(submission);
        setup();
    }

    @Test
    void testBuildUrl() {
        integrationLaunchService.buildUrl(submission, 1, integration);

        verify(submission).setIntegrationLaunchUrl(anyString());
    }

    @Test
    void testBuildUrlIntegrationNull() {
        integrationLaunchService.buildUrl(submission, 1, null);

        verify(submission, never()).setIntegrationLaunchUrl(anyString());
    }

}
