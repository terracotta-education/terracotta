package edu.iu.terracotta.service.app.async;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;

/**
 * The LMS assignment sync run on a launch (see ExperimentServiceImpl's getExperiments): lists the
 * course's LMS assignments once and, for an instructor, first checks them for a course copy no
 * notice reported (see ExperimentCopyCandidateService's stageFromCopiedAssignments). When it finds
 * one, it recreates the copied experiments as that instructor instead of running the rest of the
 * sync, which would mark the copied assignments obsolete; the obsolete-assignment check then waits
 * until recreation has finished (see ExperimentCopyCandidateService's hasUnfinishedForContext).
 * Otherwise it runs the usual restore and obsolete-assignment checks.
 */
public interface LmsAssignmentSyncService {

    /**
     * Runs the sync now, so that a copy it finds is already staged when this returns.
     */
    void sync(SecuredInfo securedInfo);

    /**
     * Runs the sync in the background.
     */
    void syncInBackground(SecuredInfo securedInfo);

}
