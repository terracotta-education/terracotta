package edu.iu.terracotta.dao.model.enums.distribute;

/**
 * The overall state of recreating a copied course's experiments, as shown to the instructor on
 * their first launch into that course (see ExperimentCopyCandidateService.getCopyStatus).
 */
public enum ExperimentCopyStatus {

    // nothing was copied into this course, or its result has already been shown
    NONE,
    IN_PROGRESS,
    COMPLETE,
    // at least one experiment could not be recreated
    ERROR,
    // a failed recreation is waiting to be retried, but the launching instructor has to
    // (re-)authorize LMS access first - only returned by the retry endpoint, which refuses to run
    // until then, so the failure alert isn't shown for a retry that never happened
    AUTHORIZATION_REQUIRED

}
