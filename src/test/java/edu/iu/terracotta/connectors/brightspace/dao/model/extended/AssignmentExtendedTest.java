package edu.iu.terracotta.connectors.brightspace.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.brightspace.io.model.Assignment;
import edu.iu.terracotta.connectors.brightspace.io.model.Availability;
import edu.iu.terracotta.connectors.brightspace.io.model.DropboxFolder;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsExternalToolFields;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;

public class AssignmentExtendedTest {

    private static final String VALID_ISO_INSTANT = "2024-01-01T10:15:30Z";

    private AssignmentExtended withDropboxFolder(DropboxFolder dropboxFolder) {
        return AssignmentExtended.builder()
            .assignment(Assignment.builder().dropboxFolder(dropboxFolder).build())
            .build();
    }

    // ---------- getId / setId ----------

    @Test
    public void testGetIdWithNullDropboxFolderReturnsNull() {
        AssignmentExtended assignmentExtended = withDropboxFolder(null);

        assertNull(assignmentExtended.getId());
    }

    @Test
    public void testGetIdReturnsDropboxFolderId() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().id(42L).build());

        assertEquals("42", assignmentExtended.getId());
    }

    @Test
    public void testSetIdWithBlankIdDoesNothing() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().id(1L).build());

        assignmentExtended.setId("  ");

        assertEquals("1", assignmentExtended.getId());
    }

    @Test
    public void testSetIdWithNullDropboxFolderDoesNotThrow() {
        AssignmentExtended assignmentExtended = withDropboxFolder(null);

        assignmentExtended.setId("100");

        assertNull(assignmentExtended.getId());
    }

    @Test
    public void testSetIdSetsDropboxFolderId() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().id(1L).build());

        assignmentExtended.setId("100");

        assertEquals("100", assignmentExtended.getId());
    }

    // ---------- getName / setName ----------

    @Test
    public void testGetNameWithNullDropboxFolderReturnsNull() {
        AssignmentExtended assignmentExtended = withDropboxFolder(null);

        assertNull(assignmentExtended.getName());
    }

    @Test
    public void testGetNameReturnsDropboxFolderName() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().name("Assignment 1").build());

        assertEquals("Assignment 1", assignmentExtended.getName());
    }

    @Test
    public void testSetNameWithNullDropboxFolderDoesNotThrow() {
        AssignmentExtended assignmentExtended = withDropboxFolder(null);

        assignmentExtended.setName("Assignment 1");

        assertNull(assignmentExtended.getName());
    }

    @Test
    public void testSetNameSetsDropboxFolderName() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().build());

        assignmentExtended.setName("Assignment 2");

        assertEquals("Assignment 2", assignmentExtended.getName());
    }

    // ---------- isPublished / setPublished ----------

    @Test
    public void testIsPublishedWithNullDropboxFolderReturnsFalse() {
        AssignmentExtended assignmentExtended = withDropboxFolder(null);

        assertFalse(assignmentExtended.isPublished());
    }

    @Test
    public void testIsPublishedWithNullIsHiddenReturnsFalse() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().build());

        assertFalse(assignmentExtended.isPublished());
    }

    @Test
    public void testIsPublishedWithIsHiddenFalseReturnsTrue() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().isHidden(false).build());

        assertTrue(assignmentExtended.isPublished());
    }

    @Test
    public void testIsPublishedWithIsHiddenTrueReturnsFalse() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().isHidden(true).build());

        assertFalse(assignmentExtended.isPublished());
    }

    @Test
    public void testSetPublishedWithNullDropboxFolderDoesNotThrow() {
        AssignmentExtended assignmentExtended = withDropboxFolder(null);

        assignmentExtended.setPublished(true);

        assertFalse(assignmentExtended.isPublished());
    }

    @Test
    public void testSetPublishedTrueSetsIsHiddenFalse() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().isHidden(true).build());

        assignmentExtended.setPublished(true);

        assertFalse(assignmentExtended.getAssignment().getDropboxFolder().getIsHidden());
    }

    @Test
    public void testSetPublishedFalseSetsIsHiddenTrue() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().isHidden(false).build());

        assignmentExtended.setPublished(false);

        assertTrue(assignmentExtended.getAssignment().getDropboxFolder().getIsHidden());
    }

    // ---------- getDueAt (parseStringToDate) ----------

    @Test
    public void testGetDueAtWithNullDropboxFolderReturnsNull() {
        AssignmentExtended assignmentExtended = withDropboxFolder(null);

        assertNull(assignmentExtended.getDueAt());
    }

    @Test
    public void testGetDueAtWithNullDueDateReturnsNull() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().dueDate(null).build());

        assertNull(assignmentExtended.getDueAt());
    }

    @Test
    public void testGetDueAtWithBlankDueDateReturnsNull() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().dueDate("   ").build());

        assertNull(assignmentExtended.getDueAt());
    }

    @Test
    public void testGetDueAtParsesValidIsoInstantString() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().dueDate(VALID_ISO_INSTANT).build());

        Date dueAt = assignmentExtended.getDueAt();

        assertNotNull(dueAt);
        assertEquals(Date.from(Instant.parse(VALID_ISO_INSTANT)), dueAt);
    }

    @Test
    public void testGetDueAtWithMalformedDueDateReturnsNullAndDoesNotThrow() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().dueDate("not-a-date").build());

        assertNull(assignmentExtended.getDueAt());
    }

    // ---------- setDueAt (parseDateToString) ----------

    @Test
    public void testSetDueAtWithNullDropboxFolderDoesNotThrow() {
        AssignmentExtended assignmentExtended = withDropboxFolder(null);

        assignmentExtended.setDueAt(new Date());

        assertNull(assignmentExtended.getDueAt());
    }

    @Test
    public void testSetDueAtWithNullDateSetsDueDateToNull() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().dueDate(VALID_ISO_INSTANT).build());

        assignmentExtended.setDueAt(null);

        assertNull(assignmentExtended.getAssignment().getDropboxFolder().getDueDate());
    }

    @Test
    public void testSetDueAtWithNonNullDateRoundTripsThroughDropboxFolder() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().build());
        Date dueAt = Date.from(Instant.parse(VALID_ISO_INSTANT));

        assignmentExtended.setDueAt(dueAt);

        assertEquals(dueAt, assignmentExtended.getDueAt());
    }

    // ---------- getSubmissionTypes / setSubmissionTypes ----------

    @Test
    public void testGetSubmissionTypesWithNullDropboxFolderReturnsEmptyList() {
        AssignmentExtended assignmentExtended = withDropboxFolder(null);

        assertEquals(List.of(), assignmentExtended.getSubmissionTypes());
    }

    @Test
    public void testGetSubmissionTypesReturnsSingleElementList() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().submissionType(2).build());

        assertEquals(List.of("2"), assignmentExtended.getSubmissionTypes());
    }

    @Test
    public void testSetSubmissionTypesWithEmptyListDoesNothing() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().submissionType(1).build());

        assignmentExtended.setSubmissionTypes(List.of());

        assertEquals(List.of("1"), assignmentExtended.getSubmissionTypes());
    }

    @Test
    public void testSetSubmissionTypesSetsDropboxFolderSubmissionType() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().build());

        assignmentExtended.setSubmissionTypes(List.of("3"));

        assertEquals(List.of("3"), assignmentExtended.getSubmissionTypes());
    }

    // ---------- getPointsPossible / setPointsPossible ----------

    @Test
    public void testGetPointsPossibleWithNullLineItemReturnsNull() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(Assignment.builder().lineItem(null).build())
            .build();

        assertNull(assignmentExtended.getPointsPossible());
    }

    @Test
    public void testGetPointsPossibleReturnsScoreMaximum() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(Assignment.builder().lineItem(LineItem.builder().scoreMaximum(10F).build()).build())
            .build();

        assertEquals(10F, assignmentExtended.getPointsPossible());
    }

    @Test
    public void testSetPointsPossibleWithNonNullValueSetsScoreMaximum() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(Assignment.builder().lineItem(LineItem.builder().build()).build())
            .build();

        assignmentExtended.setPointsPossible(25F);

        assertEquals(25F, assignmentExtended.getPointsPossible());
    }

    @Test
    public void testSetPointsPossibleWithNullValueDefaultsToZero() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .assignment(Assignment.builder().lineItem(LineItem.builder().scoreMaximum(5F).build()).build())
            .build();

        assignmentExtended.setPointsPossible(null);

        assertEquals(0F, assignmentExtended.getPointsPossible());
    }

    // ---------- getLockAt / setLockAt ----------

    @Test
    public void testGetLockAtWithNullDropboxFolderReturnsNull() {
        AssignmentExtended assignmentExtended = withDropboxFolder(null);

        assertNull(assignmentExtended.getLockAt());
    }

    @Test
    public void testGetLockAtWithNullAvailabilityReturnsNull() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().availability(null).build());

        assertNull(assignmentExtended.getLockAt());
    }

    @Test
    public void testGetLockAtParsesValidIsoInstantString() {
        AssignmentExtended assignmentExtended = withDropboxFolder(
            DropboxFolder.builder().availability(Availability.builder().endDate(VALID_ISO_INSTANT).build()).build()
        );

        assertEquals(Date.from(Instant.parse(VALID_ISO_INSTANT)), assignmentExtended.getLockAt());
    }

    @Test
    public void testGetLockAtWithMalformedEndDateReturnsNull() {
        AssignmentExtended assignmentExtended = withDropboxFolder(
            DropboxFolder.builder().availability(Availability.builder().endDate("not-a-date").build()).build()
        );

        assertNull(assignmentExtended.getLockAt());
    }

    @Test
    public void testSetLockAtWithNullAvailabilityDoesNotThrow() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().availability(null).build());

        assignmentExtended.setLockAt(new Date());

        assertNull(assignmentExtended.getLockAt());
    }

    @Test
    public void testSetLockAtWithNonNullDateRoundTripsThroughAvailability() {
        AssignmentExtended assignmentExtended = withDropboxFolder(
            DropboxFolder.builder().availability(Availability.builder().build()).build()
        );
        Date lockAt = Date.from(Instant.parse(VALID_ISO_INSTANT));

        assignmentExtended.setLockAt(lockAt);

        assertEquals(lockAt, assignmentExtended.getLockAt());
    }

    // ---------- getUnlockAt / setUnlockAt ----------

    @Test
    public void testGetUnlockAtWithNullDropboxFolderReturnsNull() {
        AssignmentExtended assignmentExtended = withDropboxFolder(null);

        assertNull(assignmentExtended.getUnlockAt());
    }

    @Test
    public void testGetUnlockAtWithNullAvailabilityReturnsNull() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().availability(null).build());

        assertNull(assignmentExtended.getUnlockAt());
    }

    @Test
    public void testGetUnlockAtParsesValidIsoInstantString() {
        AssignmentExtended assignmentExtended = withDropboxFolder(
            DropboxFolder.builder().availability(Availability.builder().startDate(VALID_ISO_INSTANT).build()).build()
        );

        assertEquals(Date.from(Instant.parse(VALID_ISO_INSTANT)), assignmentExtended.getUnlockAt());
    }

    @Test
    public void testSetUnlockAtWithNullAvailabilityDoesNotThrow() {
        AssignmentExtended assignmentExtended = withDropboxFolder(DropboxFolder.builder().availability(null).build());

        assignmentExtended.setUnlockAt(new Date());

        assertNull(assignmentExtended.getUnlockAt());
    }

    @Test
    public void testSetUnlockAtWithNonNullDateRoundTripsThroughAvailability() {
        AssignmentExtended assignmentExtended = withDropboxFolder(
            DropboxFolder.builder().availability(Availability.builder().build()).build()
        );
        Date unlockAt = Date.from(Instant.parse(VALID_ISO_INSTANT));

        assignmentExtended.setUnlockAt(unlockAt);

        assertEquals(unlockAt, assignmentExtended.getUnlockAt());
    }

    // ---------- pass-through overrides ----------

    @Test
    public void testGetAndSetSecureParamsDelegateToSuper() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        assignmentExtended.setSecureParams("secure-params");

        assertEquals("secure-params", assignmentExtended.getSecureParams());
    }

    @Test
    public void testGetAllowedAttemptsDefaultsToNegativeOne() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        assertEquals(-1, assignmentExtended.getAllowedAttempts());
    }

    @Test
    public void testGetAndSetAllowedAttemptsDelegateToSuper() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        assignmentExtended.setAllowedAttempts(5);

        assertEquals(5, assignmentExtended.getAllowedAttempts());
    }

    @Test
    public void testIsCanSubmitDefaultsToTrue() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        assertTrue(assignmentExtended.isCanSubmit());
    }

    @Test
    public void testIsAndSetCanSubmitDelegateToSuper() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        assignmentExtended.setCanSubmit(false);

        assertFalse(assignmentExtended.isCanSubmit());
    }

    @Test
    public void testGetLmsExternalToolFieldsAlwaysReturnsNull() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder()
            .lmsExternalToolFields(LmsExternalToolFields.builder().url("http://example.com").build())
            .build();

        assertNull(assignmentExtended.getLmsExternalToolFields());
    }

    @Test
    public void testGetAndSetMetadataDelegateToSuper() {
        AssignmentExtended assignmentExtended = AssignmentExtended.builder().build();

        assignmentExtended.setMetadata("{\"key\":\"value\"}");

        assertEquals("{\"key\":\"value\"}", assignmentExtended.getMetadata());
    }

    // ---------- from() ----------

    @Test
    public void testFromMapsFieldsToLmsAssignment() {
        AssignmentExtended assignmentExtended = withDropboxFolder(
            DropboxFolder.builder()
                .id(7L)
                .name("Assignment Name")
                .isHidden(false)
                .submissionType(1)
                .build()
        );
        assignmentExtended.setAllowedAttempts(3);
        assignmentExtended.setCanSubmit(true);
        assignmentExtended.setSecureParams("params");
        assignmentExtended.setMetadata("meta");
        assignmentExtended.getAssignment().setLineItem(LineItem.builder().scoreMaximum(50F).build());

        LmsAssignment lmsAssignment = assignmentExtended.from();

        assertNotNull(lmsAssignment);
        assertEquals("7", lmsAssignment.getId());
        assertEquals("Assignment Name", lmsAssignment.getName());
        assertTrue(lmsAssignment.isPublished());
        assertEquals(List.of("1"), lmsAssignment.getSubmissionTypes());
        assertEquals(3, lmsAssignment.getAllowedAttempts());
        assertTrue(lmsAssignment.isCanSubmit());
        assertEquals("params", lmsAssignment.getSecureParams());
        assertEquals("meta", lmsAssignment.getMetadata());
        assertEquals(50F, lmsAssignment.getPointsPossible());
        assertNull(lmsAssignment.getLmsExternalToolFields());
        assertNull(lmsAssignment.getDueAt());
        assertNull(lmsAssignment.getLockAt());
        assertNull(lmsAssignment.getUnlockAt());
        assertEquals(AssignmentExtended.class, lmsAssignment.getType());
    }

    // ---------- of(LmsAssignment) ----------

    @Test
    public void testOfWithNullLmsAssignmentReturnsEmptyAssignmentExtended() {
        AssignmentExtended assignmentExtended = AssignmentExtended.of(null);

        assertNotNull(assignmentExtended);
    }

    @Test
    public void testOfWithLmsAssignmentCopiesFieldsWithNullDates() {
        LmsAssignment lmsAssignment = LmsAssignment.builder()
            .allowedAttempts(4)
            .canSubmit(false)
            .dueAt(null)
            .id("55")
            .lockAt(null)
            .unlockAt(null)
            .metadata("meta-data")
            .name("Name")
            .pointsPossible(15F)
            .published(true)
            .secureParams("secure")
            .submissionTypes(List.of("2"))
            .build();

        AssignmentExtended assignmentExtended = AssignmentExtended.of(lmsAssignment);

        assertNotNull(assignmentExtended);
        assertEquals(4, assignmentExtended.getAllowedAttempts());
        assertFalse(assignmentExtended.isCanSubmit());
        assertNull(assignmentExtended.getDueAt());
        assertEquals("55", assignmentExtended.getId());
        assertNull(assignmentExtended.getLockAt());
        assertNull(assignmentExtended.getUnlockAt());
        assertEquals("meta-data", assignmentExtended.getMetadata());
        assertEquals("Name", assignmentExtended.getName());
        assertEquals(15F, assignmentExtended.getPointsPossible());
        assertTrue(assignmentExtended.isPublished());
        assertEquals("secure", assignmentExtended.getSecureParams());
        assertEquals(List.of("2"), assignmentExtended.getSubmissionTypes());
    }

    @Test
    public void testOfWithNonNullDueAtRoundTripsThroughDropboxFolder() {
        Date dueAt = Date.from(Instant.parse(VALID_ISO_INSTANT));
        LmsAssignment lmsAssignment = LmsAssignment.builder()
            .dueAt(dueAt)
            .build();

        AssignmentExtended assignmentExtended = AssignmentExtended.of(lmsAssignment);

        assertEquals(dueAt, assignmentExtended.getDueAt());
    }

}
