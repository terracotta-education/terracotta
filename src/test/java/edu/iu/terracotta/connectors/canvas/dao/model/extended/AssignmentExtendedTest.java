package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsExternalToolFields;
import edu.ksu.canvas.model.assignment.Assignment;

public class AssignmentExtendedTest {

    // -- getPointsPossible -------------------------------------------------------------------

    @Test
    public void testGetPointsPossibleReturnsNullWhenAssignmentPointsPossibleIsNull() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        assertNull(assignmentExtended.getAssignment().getPointsPossible());
        assertNull(assignmentExtended.getPointsPossible());
    }

    @Test
    public void testGetPointsPossibleReturnsFloatValueWhenSet() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();
        assignmentExtended.getAssignment().setPointsPossible(12.5);

        assertEquals(12.5f, assignmentExtended.getPointsPossible(), 0.0f);
    }

    // -- getLmsExternalToolFields --------------------------------------------------------------

    @Test
    public void testGetLmsExternalToolFieldsReturnsNullWhenExternalToolTagAttributesIsNull() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        assertNull(assignmentExtended.getAssignment().getExternalToolTagAttributes());
        assertNull(assignmentExtended.getLmsExternalToolFields());
    }

    @Test
    public void testGetLmsExternalToolFieldsReturnsFieldsWhenExternalToolTagAttributesIsSet() {
        Assignment assignment = new Assignment();
        Assignment.ExternalToolTagAttribute attribute = assignment.new ExternalToolTagAttribute();
        attribute.setUrl("https://tool.example.com/launch");
        attribute.setResourceLinkId("rlid-existing");
        assignment.setExternalToolTagAttributes(attribute);

        AssignmentExtended assignmentExtended = AssignmentExtended.builder().assignment(assignment).build();

        LmsExternalToolFields lmsExternalToolFields = assignmentExtended.getLmsExternalToolFields();

        assertNotNull(lmsExternalToolFields);
        assertEquals("https://tool.example.com/launch", lmsExternalToolFields.getUrl());
        assertEquals("rlid-existing", lmsExternalToolFields.getResourceLinkId());
    }

    @Test
    public void testGetIdReturnsNullWhenAssignmentIdNeverSet() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        assertNull(assignmentExtended.getId());
    }

    // -- from() ----------------------------------------------------------------------------------

    @Test
    public void testFromAggregatesAllFields() {
        Date dueAt = new Date(1000L);
        Date lockAt = new Date(2000L);
        Date unlockAt = new Date(3000L);

        Assignment assignment = new Assignment();
        Assignment.ExternalToolTagAttribute attribute = assignment.new ExternalToolTagAttribute();
        attribute.setUrl("https://tool.example.com/launch");
        attribute.setResourceLinkId("rlid-from");
        assignment.setExternalToolTagAttributes(attribute);

        AssignmentExtended assignmentExtended = AssignmentExtended.builder().assignment(assignment).build();
        assignmentExtended.setId("50");
        assignmentExtended.setName("Assignment Name");
        assignmentExtended.setPublished(true);
        assignmentExtended.setDueAt(dueAt);
        assignmentExtended.setSubmissionTypes(List.of("online_text_entry"));
        assignmentExtended.setPointsPossible(15.0f);
        assignmentExtended.setLockAt(lockAt);
        assignmentExtended.setUnlockAt(unlockAt);
        assignmentExtended.setSecureParams("secure-params");
        assignmentExtended.setAllowedAttempts(2);
        assignmentExtended.setCanSubmit(true);

        LmsAssignment lmsAssignment = assignmentExtended.from();

        assertNotNull(lmsAssignment);
        assertEquals("50", lmsAssignment.getId());
        assertEquals("Assignment Name", lmsAssignment.getName());
        assertTrue(lmsAssignment.isPublished());
        assertEquals(dueAt, lmsAssignment.getDueAt());
        assertEquals(List.of("online_text_entry"), lmsAssignment.getSubmissionTypes());
        assertEquals(15.0f, lmsAssignment.getPointsPossible(), 0.0f);
        assertEquals(lockAt, lmsAssignment.getLockAt());
        assertEquals(unlockAt, lmsAssignment.getUnlockAt());
        assertEquals("secure-params", lmsAssignment.getSecureParams());
        assertEquals(2, lmsAssignment.getAllowedAttempts());
        assertTrue(lmsAssignment.isCanSubmit());
        assertEquals(AssignmentExtended.class, lmsAssignment.getType());
        assertNotNull(lmsAssignment.getLmsExternalToolFields());
        assertEquals("https://tool.example.com/launch", lmsAssignment.getLmsExternalToolFields().getUrl());
        assertEquals("rlid-from", lmsAssignment.getLmsExternalToolFields().getResourceLinkId());
    }

    // -- of() ------------------------------------------------------------------------------------

    @Test
    public void testOfReturnsDefaultInstanceWhenLmsAssignmentIsNull() {
        AssignmentExtended assignmentExtended = AssignmentExtended.of(null);

        assertNotNull(assignmentExtended);
        assertNotNull(assignmentExtended.getAssignment());
        assertNull(assignmentExtended.getName());
        assertFalse(assignmentExtended.isPublished());
    }

    @Test
    public void testOfAggregatesAllFieldsAndPopulatesExternalToolTagAttributes() {
        // Because AssignmentExtended.of(...) always starts from a brand-new
        // AssignmentExtended.builder().build() (whose wrapped Canvas Assignment always
        // has null externalToolTagAttributes), the "field already set" check at
        // AssignmentExtended.java line 199 is always true in practice when invoked
        // through this public factory method - there is no way, via of(...) alone, to
        // reach the branch where an ExternalToolTagAttribute already exists and is left
        // untouched/not reconstructed. This test exercises the (always-taken) "absent"
        // path: the inner ExternalToolTagAttribute object is constructed fresh and
        // populated from the supplied LmsExternalToolFields.
        Date dueAt = new Date(4000L);
        Date lockAt = new Date(5000L);
        Date unlockAt = new Date(6000L);

        LmsAssignment lmsAssignment = LmsAssignment.builder()
            .id("111")
            .name("Quiz 1")
            .published(true)
            .dueAt(dueAt)
            .submissionTypes(List.of("online_upload"))
            .pointsPossible(20.0f)
            .lockAt(lockAt)
            .unlockAt(unlockAt)
            .gradingType("points")
            .secureParams("secure-params-value")
            .allowedAttempts(3)
            .canSubmit(true)
            .lmsExternalToolFields(
                LmsExternalToolFields.builder()
                    .url("https://tool.example.com/launch")
                    .resourceLinkId("rlid-1")
                    .build()
            )
            .build();

        AssignmentExtended assignmentExtended = AssignmentExtended.of(lmsAssignment);

        assertNotNull(assignmentExtended);
        assertEquals("111", assignmentExtended.getId());
        assertEquals("Quiz 1", assignmentExtended.getName());
        assertTrue(assignmentExtended.isPublished());
        assertEquals(dueAt, assignmentExtended.getDueAt());
        assertEquals(List.of("online_upload"), assignmentExtended.getSubmissionTypes());
        assertEquals(20.0f, assignmentExtended.getPointsPossible(), 0.0f);
        assertEquals(lockAt, assignmentExtended.getLockAt());
        assertEquals(unlockAt, assignmentExtended.getUnlockAt());
        assertEquals("points", assignmentExtended.getGradingType());
        assertEquals("secure-params-value", assignmentExtended.getSecureParams());
        assertEquals(3, assignmentExtended.getAllowedAttempts());
        assertTrue(assignmentExtended.isCanSubmit());

        LmsExternalToolFields lmsExternalToolFields = assignmentExtended.getLmsExternalToolFields();
        assertNotNull(lmsExternalToolFields);
        assertEquals("https://tool.example.com/launch", lmsExternalToolFields.getUrl());
        assertEquals("rlid-1", lmsExternalToolFields.getResourceLinkId());
    }

    @Test
    public void testOfWithExternalToolFieldsContainingNullUrlAndResourceLinkId() {
        // Covers the case where the supplied LmsExternalToolFields container is present
        // but its inner url/resourceLinkId values are absent (null). The inner
        // ExternalToolTagAttribute should still be constructed and populated (with
        // nulls), rather than throwing.
        LmsAssignment lmsAssignment = LmsAssignment.builder()
            .id("222")
            .lmsExternalToolFields(LmsExternalToolFields.builder().build())
            .build();

        AssignmentExtended assignmentExtended = AssignmentExtended.of(lmsAssignment);

        assertNotNull(assignmentExtended.getAssignment().getExternalToolTagAttributes());
        LmsExternalToolFields lmsExternalToolFields = assignmentExtended.getLmsExternalToolFields();
        assertNotNull(lmsExternalToolFields);
        assertNull(lmsExternalToolFields.getUrl());
        assertNull(lmsExternalToolFields.getResourceLinkId());
    }

    @Test
    public void testOfWithNullLmsExternalToolFieldsLeavesExternalToolTagAttributesUnset() {
        LmsAssignment lmsAssignment = LmsAssignment.builder()
            .id("333")
            .build();

        AssignmentExtended assignmentExtended = AssignmentExtended.of(lmsAssignment);

        assertNotNull(assignmentExtended);
        assertEquals("333", assignmentExtended.getId());
        assertNull(assignmentExtended.getAssignment().getExternalToolTagAttributes());
        assertNull(assignmentExtended.getLmsExternalToolFields());
    }

    @Test
    public void testOfReturnsNullIdWhenLmsAssignmentIdIsNull() {
        LmsAssignment lmsAssignment = LmsAssignment.builder()
            .lmsExternalToolFields(LmsExternalToolFields.builder().build())
            .build();

        AssignmentExtended assignmentExtended = AssignmentExtended.of(lmsAssignment);

        assertNotNull(assignmentExtended);
        assertNull(assignmentExtended.getId());
    }

}
