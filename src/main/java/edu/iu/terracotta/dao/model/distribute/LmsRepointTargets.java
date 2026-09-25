package edu.iu.terracotta.dao.model.distribute;

import java.util.Map;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import lombok.Builder;
import lombok.Value;

/**
 * LMS assignments that already exist in a course - because the LMS copied them along with the
 * course - which an import should re-point at what it recreates, instead of creating new ones
 * (see ExperimentCopyCandidateServiceImpl). Empty for a normal (manual zip upload) import.
 * Also carries which copy candidate the import is for, when it's recreating one.
 */
@Value
@Builder(toBuilder = true)
public class LmsRepointTargets {

    // source (old) Assignment ID -> the copied LMS assignment that launched it
    @Builder.Default
    Map<Long, LmsAssignment> assignments = Map.of();

    // the copied LMS assignment that launched the source experiment's consent document, if any
    LmsAssignment consentAssignment;

    // the copy candidate this import recreates, if any - LMS assignments the import creates are
    // recorded against it, so a retry can remove them first (see ExperimentCopyCreatedAssignment)
    Long copyCandidateId;

    public static LmsRepointTargets none() {
        return LmsRepointTargets.builder().build();
    }

    public static LmsRepointTargets ofAssignments(Map<Long, LmsAssignment> assignments) {
        return LmsRepointTargets.builder().assignments(assignments).build();
    }

}
