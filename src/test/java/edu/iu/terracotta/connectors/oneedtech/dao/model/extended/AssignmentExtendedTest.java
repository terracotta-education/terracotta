package edu.iu.terracotta.connectors.oneedtech.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.oneedtech.dao.model.lms.Assignment;

public class AssignmentExtendedTest {

    private Assignment assignment() {
        return Assignment.builder()
            .id("assignment-id-1")
            .resourceLinkId("resource-link-id-1")
            .label("Assignment Label")
            .scoreMaximum(87.5f)
            .build();
    }

    // getSecureParams

    @Test
    public void testGetSecureParamsEncodesResourceLinkIdAsBase64Json() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        String secureParams = assignmentExtended.getSecureParams();
        String decoded = new String(Base64.getDecoder().decode(secureParams));

        String expected = String.format(
            "{\"lti_assignment_id\":\"%s\"}",
            "resource-link-id-1"
        );

        assertEquals(expected, decoded);
    }

    @Test
    public void testGetSecureParamsWithNullResourceLinkId() {
        Assignment assignment = Assignment.builder()
            .resourceLinkId(null)
            .build();
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment)
            .build();

        String secureParams = assignmentExtended.getSecureParams();
        String decoded = new String(Base64.getDecoder().decode(secureParams));

        String expected = String.format(
            "{\"lti_assignment_id\":\"%s\"}",
            (Object) null
        );

        assertEquals(expected, decoded);
    }

    // getId / getName / setName

    @Test
    public void testGetIdDelegatesToAssignment() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        assertEquals("assignment-id-1", assignmentExtended.getId());
    }

    @Test
    public void testGetNameDelegatesToAssignmentLabel() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        assertEquals("Assignment Label", assignmentExtended.getName());
    }

    @Test
    public void testSetNameUpdatesAssignmentLabel() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        assignmentExtended.setName("New Label");

        assertEquals("New Label", assignmentExtended.getName());
        assertEquals("New Label", assignmentExtended.getAssignment().getLabel());
    }

    // getPointsPossible

    @Test
    public void testGetPointsPossibleDelegatesToAssignmentScoreMaximum() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        assertEquals(Float.valueOf(87.5f), assignmentExtended.getPointsPossible());
    }

    // hardcoded-constant overrides

    @Test
    public void testGetAllowedAttemptsAlwaysReturnsZero() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        assignmentExtended.setAllowedAttempts(5);

        assertEquals(0, assignmentExtended.getAllowedAttempts());
    }

    @Test
    public void testIsCanSubmitAlwaysReturnsTrue() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        assignmentExtended.setCanSubmit(false);

        assertTrue(assignmentExtended.isCanSubmit());
    }

    @Test
    public void testIsPublishedAlwaysReturnsTrue() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        assertTrue(assignmentExtended.isPublished());
    }

    @Test
    public void testGetDueAtAlwaysReturnsNull() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        assertNull(assignmentExtended.getDueAt());
    }

    @Test
    public void testGetSubmissionTypesAlwaysReturnsExternalTool() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        assertEquals(Collections.singletonList("external_tool"), assignmentExtended.getSubmissionTypes());
    }

    @Test
    public void testGetLockAtAlwaysReturnsNull() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        assertNull(assignmentExtended.getLockAt());
    }

    @Test
    public void testGetUnlockAtAlwaysReturnsNull() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        assertNull(assignmentExtended.getUnlockAt());
    }

    // default assignment (Builder.Default)

    @Test
    public void testDefaultAssignmentIsUsedWhenNoneProvided() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        assertNull(assignmentExtended.getId());
        assertNull(assignmentExtended.getName());
    }

    // from()

    @Test
    public void testFromConvertsAndReturnsSameInstanceWithMappedFields() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(assignment())
            .build();

        LmsAssignment converted = assignmentExtended.from();

        assertSame(assignmentExtended, converted);
        assertEquals(Assignment.class, converted.getType());
        assertEquals("assignment-id-1", converted.getId());
        assertEquals("Assignment Label", converted.getName());
        assertTrue(converted.isPublished());
        assertEquals(assignmentExtended.getSecureParams(), converted.getSecureParams());
        assertNull(converted.getDueAt());

        List<String> submissionTypes = converted.getSubmissionTypes();
        assertEquals(Collections.singletonList("external_tool"), submissionTypes);
        assertEquals(Float.valueOf(87.5f), converted.getPointsPossible());
        assertNull(converted.getLockAt());
        assertNull(converted.getUnlockAt());
    }

}
