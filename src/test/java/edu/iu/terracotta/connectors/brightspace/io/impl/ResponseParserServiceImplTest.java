package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageQuickLink;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public class ResponseParserServiceImplTest {

    private ResponseParserServiceImpl responseParserService;

    @BeforeEach
    public void beforeEach() {
        responseParserService = new ResponseParserServiceImpl();
    }

    private Response responseWithContent(String content) {
        return Response.builder().responseCode(200).content(content).build();
    }

    // parseToList

    @Test
    public void testParseToListReturnsEmptyListWhenContentIsNull() {
        List<LtiAdvantageQuickLink> result = responseParserService.parseToList(new TypeReference<List<LtiAdvantageQuickLink>>() {}, responseWithContent(null));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseToListReturnsEmptyListWhenContentIsBlank() {
        List<LtiAdvantageQuickLink> result = responseParserService.parseToList(new TypeReference<List<LtiAdvantageQuickLink>>() {}, responseWithContent("   "));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseToListParsesValidJsonArray() {
        String content = "[{\"LinkId\":123,\"PublicUrl\":\"https://one.example.com\"},{\"LinkId\":456,\"PublicUrl\":\"https://two.example.com\"}]";

        List<LtiAdvantageQuickLink> result = responseParserService.parseToList(new TypeReference<List<LtiAdvantageQuickLink>>() {}, responseWithContent(content));

        assertEquals(2, result.size());
        assertEquals(123L, result.get(0).getLinkId());
        assertEquals("https://one.example.com", result.get(0).getPublicUrl());
        assertEquals(456L, result.get(1).getLinkId());
        assertEquals("https://two.example.com", result.get(1).getPublicUrl());
    }

    @Test
    public void testParseToListThrowsRuntimeExceptionOnMalformedJson() {
        assertThrows(RuntimeException.class, () -> responseParserService.parseToList(new TypeReference<List<LtiAdvantageQuickLink>>() {}, responseWithContent("{not valid json")));
    }

    // parseToObject

    @Test
    public void testParseToObjectReturnsEmptyOptionalWhenContentIsNull() {
        Optional<LtiAdvantageQuickLink> result = responseParserService.parseToObject(LtiAdvantageQuickLink.class, responseWithContent(null));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseToObjectReturnsEmptyOptionalWhenContentIsBlank() {
        Optional<LtiAdvantageQuickLink> result = responseParserService.parseToObject(LtiAdvantageQuickLink.class, responseWithContent(""));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseToObjectParsesValidJsonObject() {
        String content = "{\"LinkId\":123,\"PublicUrl\":\"https://one.example.com\"}";

        Optional<LtiAdvantageQuickLink> result = responseParserService.parseToObject(LtiAdvantageQuickLink.class, responseWithContent(content));

        assertTrue(result.isPresent());
        assertEquals(123L, result.get().getLinkId());
        assertEquals("https://one.example.com", result.get().getPublicUrl());
    }

    @Test
    public void testParseToObjectIgnoresUnknownProperties() {
        String content = "{\"LinkId\":123,\"PublicUrl\":\"https://one.example.com\",\"SomeUnknownField\":\"value\"}";

        Optional<LtiAdvantageQuickLink> result = responseParserService.parseToObject(LtiAdvantageQuickLink.class, responseWithContent(content));

        assertTrue(result.isPresent());
        assertEquals(123L, result.get().getLinkId());
    }

    @Test
    public void testParseToObjectThrowsRuntimeExceptionOnMalformedJson() {
        assertThrows(RuntimeException.class, () -> responseParserService.parseToObject(LtiAdvantageQuickLink.class, responseWithContent("{not valid json")));
    }

    // parseToMap

    @Test
    public void testParseToMapReturnsEmptyMapWhenContentIsNull() {
        Map<String, LtiAdvantageQuickLink> result = responseParserService.parseToMap(LtiAdvantageQuickLink.class, responseWithContent(null));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseToMapReturnsEmptyMapWhenContentIsBlank() {
        Map<String, LtiAdvantageQuickLink> result = responseParserService.parseToMap(LtiAdvantageQuickLink.class, responseWithContent("\t"));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseToMapParsesValidJsonObject() {
        String content = "{\"first\":{\"LinkId\":123,\"PublicUrl\":\"https://one.example.com\"}}";

        Map<String, LtiAdvantageQuickLink> result = responseParserService.parseToMap(LtiAdvantageQuickLink.class, responseWithContent(content));

        assertEquals(1, result.size());
        assertEquals(123L, result.get("first").getLinkId());
        assertEquals("https://one.example.com", result.get("first").getPublicUrl());
    }

    @Test
    public void testParseToMapThrowsRuntimeExceptionOnMalformedJson() {
        assertThrows(RuntimeException.class, () -> responseParserService.parseToMap(LtiAdvantageQuickLink.class, responseWithContent("{not valid json")));
    }

    // getJsonParser

    @Test
    public void testGetJsonParserExcludesNullFieldsWhenSerializeNullsIsFalse() {
        JsonMapper jsonMapper = ResponseParserServiceImpl.getJsonParser(false);

        String json = jsonMapper.writeValueAsString(LtiAdvantageQuickLink.builder().linkId(1L).publicUrl(null).build());

        assertFalse(json.contains("PublicUrl"));
        assertTrue(json.contains("LinkId"));
    }

    @Test
    public void testGetJsonParserIncludesNullFieldsWhenSerializeNullsIsTrue() {
        JsonMapper jsonMapper = ResponseParserServiceImpl.getJsonParser(true);

        String json = jsonMapper.writeValueAsString(LtiAdvantageQuickLink.builder().linkId(1L).publicUrl(null).build());

        assertTrue(json.contains("PublicUrl"));
    }

}
