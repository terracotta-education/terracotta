package edu.iu.terracotta.service.app.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.model.app.integrations.IntegrationClient;
import edu.iu.terracotta.model.app.integrations.dto.IntegrationClientDto;
import edu.iu.terracotta.service.app.integrations.impl.IntegrationClientServiceImpl;

public class IntegrationClientServiceImplTest extends BaseTest {

    @InjectMocks private IntegrationClientServiceImpl integrationClientService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    void testGetAll() {
        List<IntegrationClient> ret = integrationClientService.getAll();

        assertNotNull(ret);
        assertEquals(1, ret.size());
    }

    @Test
    void testToDtoList() {
        List<IntegrationClientDto> ret = integrationClientService.toDto(Collections.singletonList(integrationClient), "http://local.url");

        assertNotNull(ret);
        assertEquals(1, ret.size());
    }

    @Test
    void testToDtoListEmpty() {
        List<IntegrationClientDto> ret = integrationClientService.toDto(Collections.emptyList(), "http://local.url");

        assertNotNull(ret);
        assertEquals(0, ret.size());
    }

    @Test
    void testFromDto() throws IntegrationClientNotFoundException {
        IntegrationClient ret = integrationClientService.fromDto(integrationClientDto);

        assertNotNull(ret);
    }

}
