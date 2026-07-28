package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests the two overloaded, validating constructors of {@link LtiContextEntity}:
 * <ul>
 *   <li>{@code LtiContextEntity(String contextKey, ToolDeployment toolDeployment, String title, String json)}</li>
 *   <li>{@code LtiContextEntity(String contextKey, ToolDeployment toolDeployment, String title,
 *       String contextMembershipsUrl, String lineitems, String json)}</li>
 * </ul>
 * Both validate {@code contextKey} is not blank and {@code toolDeployment} is not null.
 */
public class LtiContextEntityTest {

    private ToolDeployment buildToolDeployment() {
        return ToolDeployment.builder()
            .ltiDeploymentId("lti-deployment-id")
            .build();
    }

    @Test
    public void testFourArgConstructorSetsAllFieldsCorrectly() {
        ToolDeployment toolDeployment = buildToolDeployment();

        LtiContextEntity entity = new LtiContextEntity("context-key", toolDeployment, "My Title", "{\"json\":true}");

        assertEquals("context-key", entity.getContextKey());
        assertEquals(toolDeployment, entity.getToolDeployment());
        assertEquals("My Title", entity.getTitle());
        assertEquals("{\"json\":true}", entity.getJson());
        assertNull(entity.getContext_memberships_url());
        assertNull(entity.getLineitems());
    }

    @Test
    public void testFourArgConstructorBlankContextKeyThrowsAssertionError() {
        ToolDeployment toolDeployment = buildToolDeployment();

        assertThrows(AssertionError.class, () -> new LtiContextEntity("", toolDeployment, "title", "json"));
        assertThrows(AssertionError.class, () -> new LtiContextEntity("   ", toolDeployment, "title", "json"));
        assertThrows(AssertionError.class, () -> new LtiContextEntity(null, toolDeployment, "title", "json"));
    }

    @Test
    public void testFourArgConstructorNullToolDeploymentThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new LtiContextEntity("context-key", null, "title", "json"));
    }

    @Test
    public void testSixArgConstructorSetsAllFieldsCorrectly() {
        ToolDeployment toolDeployment = buildToolDeployment();

        LtiContextEntity entity = new LtiContextEntity(
            "context-key",
            toolDeployment,
            "My Title",
            "https://example.com/memberships",
            "https://example.com/lineitems",
            "{\"json\":true}"
        );

        assertEquals("context-key", entity.getContextKey());
        assertEquals(toolDeployment, entity.getToolDeployment());
        assertEquals("My Title", entity.getTitle());
        assertEquals("https://example.com/memberships", entity.getContext_memberships_url());
        assertEquals("https://example.com/lineitems", entity.getLineitems());
        assertEquals("{\"json\":true}", entity.getJson());
    }

    @Test
    public void testSixArgConstructorBlankContextKeyThrowsAssertionError() {
        ToolDeployment toolDeployment = buildToolDeployment();

        assertThrows(AssertionError.class, () -> new LtiContextEntity("", toolDeployment, "title", "memberships", "lineitems", "json"));
        assertThrows(AssertionError.class, () -> new LtiContextEntity("   ", toolDeployment, "title", "memberships", "lineitems", "json"));
        assertThrows(AssertionError.class, () -> new LtiContextEntity(null, toolDeployment, "title", "memberships", "lineitems", "json"));
    }

    @Test
    public void testSixArgConstructorNullToolDeploymentThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new LtiContextEntity("context-key", null, "title", "memberships", "lineitems", "json"));
    }

}
