package edu.iu.terracotta.service.app.async;

public interface ExperimentCopyRecreationAsyncService {

    /**
     * Recreates every PENDING copy candidate's source Experiment in the given (destination)
     * context, in the background - see ExperimentCopyCandidateService.recreateForContext.
     */
    void recreate(long destinationContextId);

    /**
     * Same as recreate(long), acting as the given user - see
     * ExperimentCopyCandidateService.recreateForContext.
     */
    void recreate(long destinationContextId, String actingUserKey);

}
