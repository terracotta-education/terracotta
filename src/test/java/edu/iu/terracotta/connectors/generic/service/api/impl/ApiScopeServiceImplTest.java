package edu.iu.terracotta.connectors.generic.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiScope;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.exceptions.ApiScopeNotFoundException;

public class ApiScopeServiceImplTest extends BaseTest {

    @InjectMocks private ApiScopeServiceImpl apiScopeService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
    }

    @Test
    public void testGetScopeById() throws ApiScopeNotFoundException {
        ApiScope ret = apiScopeService.getScopeById(1L);

        assertEquals(apiScope, ret);
    }

    @Test
    public void testGetScopeByIdNotFound() {
        when(apiScopeRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ApiScopeNotFoundException.class, () -> apiScopeService.getScopeById(1L));
    }

    @Test
    public void testGetScopeByUUID() throws ApiScopeNotFoundException {
        ApiScope ret = apiScopeService.getScopeByUUID(UUID.randomUUID());

        assertEquals(apiScope, ret);
    }

    @Test
    public void testGetScopeByUUIDNotFound() {
        when(apiScopeRepository.findByUuid(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ApiScopeNotFoundException.class, () -> apiScopeService.getScopeByUUID(UUID.randomUUID()));

    }

    @Test
    public void testCreateScope() {
        ApiScope ret = apiScopeService.createScope(apiScope);

        assertEquals(apiScope, ret);
    }

    @Test
    public void testUpdateScope() {
        ApiScope ret = apiScopeService.updateScope(apiScope);

        assertEquals(apiScope, ret);
    }

    @Test
    public void testDeleteScope() {
        apiScopeService.deleteScope(1L);
        // Verify interaction
    }

    @Test
    public void testGetScopesForFeature() {
        List<String> ret = apiScopeService.getScopesForFeature(1L);

        assertEquals(1, ret.size());
    }

    @Test
    public void testGetNecessaryScopes() {
        Set<String> ret = apiScopeService.getNecessaryScopes(1L);

        assertEquals(1, ret.size());
    }

    @Test
    public void testGetNecessaryScopesWithSeparator() {
        String ret = apiScopeService.getNecessaryScopes(1L, ",");

        assertNotNull(ret);
    }

    @Test
    public void testGetScopesForLmsConnector() {
        List<ApiScope> ret = apiScopeService.getScopesForLmsConnector(LmsConnector.CANVAS);

        assertNotNull(ret);
    }

}