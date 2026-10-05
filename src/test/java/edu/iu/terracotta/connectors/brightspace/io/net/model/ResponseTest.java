package edu.iu.terracotta.connectors.brightspace.io.net.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ResponseTest {

    @Test
    public void testBuilderGettersAndDefault() {
        Response response = Response.builder()
            .responseCode(200)
            .next("next-url")
            .content("content-body")
            .build();

        assertEquals(200, response.getResponseCode());
        assertEquals("next-url", response.getNext());
        assertEquals("content-body", response.getContent());
        assertFalse(response.isErrorHappened());
    }

    @Test
    public void testBuilderErrorHappenedOverride() {
        Response response = Response.builder()
            .responseCode(500)
            .errorHappened(true)
            .build();

        assertTrue(response.isErrorHappened());
    }

    @Test
    public void testSetters() {
        Response response = Response.builder().build();

        response.setResponseCode(404);
        response.setNext("next-2");
        response.setContent("content-2");
        response.setErrorHappened(true);

        assertEquals(404, response.getResponseCode());
        assertEquals("next-2", response.getNext());
        assertEquals("content-2", response.getContent());
        assertTrue(response.isErrorHappened());
    }

    @Test
    public void testToString() {
        Response response = Response.builder()
            .responseCode(200)
            .next("next-url")
            .content("content-body")
            .errorHappened(false)
            .build();

        String expected = "Response{errorHappened=false, responseCode=200, next='next-url', content='content-body'}";

        assertEquals(expected, response.toString());
    }

}
