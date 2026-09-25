package edu.iu.terracotta.dao.model.distribute;

import java.util.Map;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import lombok.Builder;
import lombok.Value;

/**
 * LMS assignments that already exist in a course - because the LMS copied them along with the
 * course - which an import should re-point at what it recreates, instead of creating new ones
 * (see ExperimentCopyCandidateServiceImpl). Empty for a normal (manual zip upload) import.
 */
@Value
@Builder
public class LmsRepointTargets {

    // source (old) Assignment ID -> the copied LMS assignment that launched it
    @Builder.Default
    Map<Long, LmsAssignment> assignments = Map.of();

    // the copied LMS assignment that launched the source experiment's consent document, if any
    LmsAssignment consentAssignment;

    public static LmsRepointTargets none() {
        return LmsRepointTargets.builder().build();
    }

    public static LmsRepointTargets ofAssignments(Map<Long, LmsAssignment> assignments) {
        return LmsRepointTargets.builder().assignments(assignments).build();
    }

}
