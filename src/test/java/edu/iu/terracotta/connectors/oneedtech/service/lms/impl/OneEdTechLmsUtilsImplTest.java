package edu.iu.terracotta.connectors.oneedtech.service.lms.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;

public class OneEdTechLmsUtilsImplTest {

    private OneEdTechLmsUtilsImpl oneEdTechLmsUtils;
    private PlatformDeployment platformDeployment;

    @BeforeEach
    public void beforeEach() {
        oneEdTechLmsUtils = new OneEdTechLmsUtilsImpl();

        platformDeployment = PlatformDeployment.builder()
            .keyId(1L)
            .localUrl("https://terracotta.example.com")
            .build();
    }

    @Test
    public void testParseCourseIdReturnsUrlUnchanged() {
        String url = "https://lms.example.com/course/123";

        assertEquals(url, oneEdTechLmsUtils.parseCourseId(platformDeployment, url));
    }

    @Test
    public void testParseCourseIdReturnsNullWhenUrlIsNull() {
        assertNull(oneEdTechLmsUtils.parseCourseId(platformDeployment, null));
    }

    @Test
    public void testParseCourseIdIgnoresPlatformDeployment() {
        String url = "https://lms.example.com/course/123";

        assertEquals(url, oneEdTechLmsUtils.parseCourseId(null, url));
    }

    @Test
    public void testParseDeploymentIdThrowsUnsupportedOperationException() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> oneEdTechLmsUtils.parseDeploymentId(platformDeployment, "https://lms.example.com/course/123")
        );
    }

    @Test
    public void testSanitizeReturnsInputUnchanged() {
        String input = "<script>alert('x')</script>";

        assertEquals(input, oneEdTechLmsUtils.sanitize(input));
    }

    @Test
    public void testSanitizeReturnsNullWhenInputIsNull() {
        assertNull(oneEdTechLmsUtils.sanitize(null));
    }

}
