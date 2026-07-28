package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.model.dto.ConfigurationDto;
import edu.iu.terracotta.service.app.ConfigurationService;

public class ConfigurationControllerTest extends BaseTest {

    private ConfigurationController configurationController;
    @Mock private ConfigurationService configurationService;

    @BeforeEach
    public void beforeEach() throws NumberFormatException, TerracottaConnectorException {
        MockitoAnnotations.openMocks(this);

        setup();

        configurationController = new ConfigurationController(apiJwtService, configurationService);

        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
    }

    @Test
    void getUnauthorizedTest() throws NumberFormatException, TerracottaConnectorException {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ConfigurationDto> ret = configurationController.get(httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertNull(ret.getBody());
    }

    @Test
    void getOkTest() throws NumberFormatException, TerracottaConnectorException {
        ConfigurationDto configurationDto = ConfigurationDto.builder()
            .helpUrl("http://help.example.com")
            .build();

        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(configurationService.getConfigurations(securedInfo)).thenReturn(configurationDto);

        ResponseEntity<ConfigurationDto> ret = configurationController.get(httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(configurationDto, ret.getBody());
    }

    @Test
    void getPropagatesTerracottaConnectorExceptionTest() throws NumberFormatException, TerracottaConnectorException {
        doThrow(new TerracottaConnectorException("error")).when(apiJwtService).extractValues(httpServletRequest, false);

        assertThrows(TerracottaConnectorException.class, () -> configurationController.get(httpServletRequest));
    }

}
