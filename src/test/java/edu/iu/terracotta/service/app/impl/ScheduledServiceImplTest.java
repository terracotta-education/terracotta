package edu.iu.terracotta.service.app.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.sql.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;

public class ScheduledServiceImplTest extends BaseTest {

    @InjectMocks private ScheduledServiceImpl scheduledService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testDeleteOldTokens() {
        scheduledService.deleteOldTokens();

        verify(apiOneUseTokenRepository).deleteByCreatedAtBefore(any(Date.class));
    }

    @Test
    public void testDeleteOldNonces() {
        scheduledService.deleteOldNonces();

        verify(ltiNonceRepository).deleteByCreatedAtBefore(any(Date.class));
    }

}
