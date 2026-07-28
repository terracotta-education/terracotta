package edu.iu.terracotta.connectors.generic.dao.model.lti.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DeepLinkDtoTest {

    @Mock private HttpServletRequest request;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testConstructorFromRequestSetsFieldsFromParameters() {
        when(request.getParameter("toolLinkId")).thenReturn("link-1");
        when(request.getParameter("title")).thenReturn("Assignment Title");
        when(request.getParameter("description")).thenReturn("Assignment Description");

        DeepLinkDto deepLinkDto = new DeepLinkDto(request);

        assertEquals("link-1", deepLinkDto.getToolLinkId());
        assertEquals("Assignment Title", deepLinkDto.getTitle());
        assertEquals("Assignment Description", deepLinkDto.getDescription());
    }

    @Test
    public void testConstructorFromRequestWithAllNullParametersStoresNulls() {
        when(request.getParameter("toolLinkId")).thenReturn(null);
        when(request.getParameter("title")).thenReturn(null);
        when(request.getParameter("description")).thenReturn(null);

        DeepLinkDto deepLinkDto = new DeepLinkDto(request);

        assertNull(deepLinkDto.getToolLinkId());
        assertNull(deepLinkDto.getTitle());
        assertNull(deepLinkDto.getDescription());
    }

    @Test
    public void testConstructorFromRequestWithSomeNullParametersStoresNullsAsIs() {
        // Confirms the constructor does not default missing parameters to any
        // fallback value (e.g. empty string) - a null parameter is stored as null.
        when(request.getParameter("toolLinkId")).thenReturn("link-2");
        when(request.getParameter("title")).thenReturn(null);
        when(request.getParameter("description")).thenReturn("Some Description");

        DeepLinkDto deepLinkDto = new DeepLinkDto(request);

        assertEquals("link-2", deepLinkDto.getToolLinkId());
        assertNull(deepLinkDto.getTitle());
        assertEquals("Some Description", deepLinkDto.getDescription());
    }

}
