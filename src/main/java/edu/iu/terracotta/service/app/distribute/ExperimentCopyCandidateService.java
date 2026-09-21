package edu.iu.terracotta.service.app.distribute;

import java.util.List;
import java.util.UUID;

import io.jsonwebtoken.Claims;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateDto;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateResolutionDto;

/**
 * Detects (via an LTI Advantage Platform Notification Service "course copy" notice) that a course
 * was copied from another course Terracotta has an Experiment in, stages that as a pending
 * ExperimentCopyCandidate, and - once the instructor resolves the prompt during a live launch
 * into the destination course - drives the existing export/import pipeline to recreate the
 * selected one(s) there (re-pointing any already-copied LMS assignment in place) while running
 * the existing obsolete-assignment process against everything declined.
 */
public interface ExperimentCopyCandidateService {

    /**
     * Called at notice-time (no live session) - resolves/creates the destination context and the
     * origin context(s) named in the notice, and stages a PENDING candidate for every Experiment
     * found in those origin context(s) that doesn't already have one. Idempotent: safe to call
     * more than once for the same notice (PNS notices can be redelivered).
     */
    void stageFromNotice(Claims noticeClaims);

    /**
     * Whether the given context currently has any PENDING candidate - used to suppress the
     * obsolete-assignment check (NoticeController, ExperimentServiceImpl) until the instructor
     * has resolved the prompt via resolve(...) below. A cheap no-op for the overwhelming majority
     * of contexts, which have never been a course-copy destination at all.
     */
    boolean hasPendingForContext(long contextId);

    /**
     * Pending candidates for the current (live-launch) context - always empty once that context
     * already has any Experiment of its own, matching the same "is this course new" signal the
     * rest of the app already uses to gate the first-launch/zero-state experience.
     */
    List<CopyCandidateDto> getPendingForContext(SecuredInfo securedInfo);

    /**
     * Resolves every PENDING candidate for the current context in one action: candidates named in
     * importCandidateIds are imported (exported via ExperimentExportService, fed into the existing
     * import pipeline, with any already-copied LMS assignment re-pointed rather than duplicated);
     * every other PENDING candidate is declined. Once both are done, the existing obsolete-
     * assignment check runs exactly once - now safe, since re-pointed assignments' URLs already
     * match this context's new IDs and declined ones' stale URLs still don't.
     */
    CopyCandidateResolutionDto resolve(List<UUID> importCandidateIds, SecuredInfo securedInfo);

}
