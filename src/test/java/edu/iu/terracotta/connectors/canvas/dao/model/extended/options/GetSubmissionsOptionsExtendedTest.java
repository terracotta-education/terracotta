package edu.iu.terracotta.connectors.canvas.dao.model.extended.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

public class GetSubmissionsOptionsExtendedTest {

    @Test
    public void testConstructorSetsCanvasIdAndAssignmentIds() {
        List<String> assignmentIds = List.of("1", "2", "3");

        GetSubmissionsOptionsExtended options = new GetSubmissionsOptionsExtended("course-1", assignmentIds);

        assertEquals("course-1", options.getCanvasId());
        assertEquals(assignmentIds, options.getAssignmentIds());
        assertEquals(assignmentIds, options.getOptionsMap().get("assignment_ids[]"));
    }

    @Test
    public void testAssignmentIdsUpdatesFieldAndOptionsMapAndReturnsSameInstance() {
        GetSubmissionsOptionsExtended options = new GetSubmissionsOptionsExtended("course-1", List.of("1"));
        List<String> newIds = List.of("4", "5");

        GetSubmissionsOptionsExtended returned = (GetSubmissionsOptionsExtended) options.assignmentIds(newIds);

        assertSame(options, returned);
        assertEquals(newIds, options.getAssignmentIds());
        assertEquals(newIds, options.getOptionsMap().get("assignment_ids[]"));
    }

    @Test
    public void testUserIdEnumToStringIsLowercase() {
        assertEquals("all", GetSubmissionsOptionsExtended.UserId.ALL.toString());
    }

}
