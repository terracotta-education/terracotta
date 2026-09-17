package edu.iu.terracotta.dao.model.enums.distribute;

public enum ExperimentCopyCandidateStatus {

    PENDING,
    IMPORTING,
    IMPORTED,
    ERROR,
    // every pending candidate for the context was declined at once (e.g. "No thank you") - as
    // opposed to NOT_SELECTED, which only applies to a candidate left out of an otherwise
    // non-empty selection
    DISMISSED,
    NOT_SELECTED

}
