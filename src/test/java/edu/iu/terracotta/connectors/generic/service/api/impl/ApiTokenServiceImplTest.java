package edu.iu.terracotta.connectors.generic.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.iu.terracotta.base.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class ApiTokenServiceImplTest extends BaseTest {

    @InjectMocks private ApiTokenServiceImpl apiTokenService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
    }

    @Test
    public void testFindAndDeleteOneUseTokenTokenExists() {
        boolean ret = apiTokenService.findAndDeleteOneUseToken("testToken");

        assertTrue(ret);
        verify(apiOneUseTokenRepository).delete(apiOneUseToken);
    }

    @Test
    public void testFindAndDeleteOneUseTokenTokenDoesNotExist() {
        when(apiOneUseTokenRepository.findByToken(anyString())).thenReturn(null);

        boolean ret = apiTokenService.findAndDeleteOneUseToken("testToken");

        assertFalse(ret);
        verify(apiOneUseTokenRepository).findByToken(anyString());
    }

}