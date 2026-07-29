package edu.iu.terracotta.service.app;

import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.dao.entity.Experiment;

public interface ParticipantRosterWriteService {

    /**
     * Matches, creates, and saves participants for one page of a course roster sync, in its own
     * transaction independent of the caller's (which may span many pages/minutes for a huge
     * course) - so lock contention is scoped to one page's worth of writes instead of held for
     * the whole sync.
     *
     * @param experiment
     * @param batchUsers
     */
    void syncParticipantsPage(Experiment experiment, List<LmsUserBatch> batchUsers);

}
