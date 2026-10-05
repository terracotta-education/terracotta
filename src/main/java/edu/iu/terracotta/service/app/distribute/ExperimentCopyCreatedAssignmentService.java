package edu.iu.terracotta.service.app.distribute;

/**
 * Keeps track of the LMS assignments a copy candidate's recreation creates, independently of the
 * import's own transaction - see ExperimentCopyCreatedAssignment.
 */
public interface ExperimentCopyCreatedAssignmentService {

    /**
     * Best effort, in a transaction of its own that commits right away: a failure is logged,
     * never thrown, so it can't interrupt the import.
     */
    void recordCreated(long copyCandidateId, String lmsAssignmentId);

    /**
     * Forgets the recorded assignments, once the recreation that created them has succeeded -
     * they're then the course's real assignments, and must never be removed by a retry.
     */
    void clear(long copyCandidateId);

}
