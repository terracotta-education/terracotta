package edu.iu.terracotta.connectors.canvas.service.lms.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

public class CanvasLmsUtilsImplTest extends BaseTest {

    @InjectMocks private CanvasLmsUtilsImpl canvasLmsUtils;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testParseCourseId() throws TerracottaConnectorException {
        String ret = canvasLmsUtils.parseCourseId(platformDeployment, "https://canvas.example.edu/api/v1/courses/12345/names");

        assertEquals("12345", ret);
    }

    @Test
    public void testParseCourseIdNoMatch() throws TerracottaConnectorException {
        String ret = canvasLmsUtils.parseCourseId(platformDeployment, "https://canvas.example.edu/api/v1/other/12345/path");

        assertNull(ret);
    }

    @Test
    public void testParseCourseIdNullUrl() throws TerracottaConnectorException {
        String ret = canvasLmsUtils.parseCourseId(platformDeployment, null);

        assertNull(ret);
    }

    @Test
    public void testParseDeploymentId() {
        assertThrows(UnsupportedOperationException.class, () -> canvasLmsUtils.parseDeploymentId(platformDeployment, "https://canvas.example.edu/api/v1/courses/12345/names"));
    }

    @Test
    public void testSanitize() {
        String ret = canvasLmsUtils.sanitize("some/weird*string:with|special?chars");

        // CanvasLmsUtilsImpl performs no sanitization; the input is returned unchanged.
        assertEquals("some/weird*string:with|special?chars", ret);
    }

    @Test
    public void testSanitizeNull() {
        String ret = canvasLmsUtils.sanitize(null);

        assertNull(ret);
    }

}
