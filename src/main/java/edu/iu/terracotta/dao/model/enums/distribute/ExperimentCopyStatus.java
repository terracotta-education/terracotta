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
    ERROR

}
