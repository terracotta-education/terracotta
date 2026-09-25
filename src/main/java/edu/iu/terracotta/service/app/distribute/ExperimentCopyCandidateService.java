package edu.iu.terracotta.service.app.distribute;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.jsonwebtoken.Claims;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateDto;
import edu.iu.terracotta.dao.model.dto.distribute.CopyCandidateResolutionDto;
import edu.iu.terracotta.dao.model.dto.distribute.CopyStatusDto;

/**
 * Detects (via an LTI Advantage Platform Notification Service "course copy" notice) that a course
 * was copied from another course Terracotta has an Experiment in, stages that as a pending
 * ExperimentCopyCandidate, and then recreates the source Experiment(s) in the destination course
 * automatically, with no live session: the source course's instructor's LMS API access drives
 * the existing export/import pipeline, re-pointing each already-copied LMS assignment at its
 * recreated Assignment rather than creating a duplicate. The instructor is told how it went on
 * their first launch into the destination course.
 *
 * getPendingForContext/resolve drive the earlier flow where the instructor chose which
 * candidates to recreate at launch time. That UI is switched off (see Home.vue) but not removed.
 */
public interface ExperimentCopyCandidateService {

    /**
     * Called at notice-time (no live session) - resolves/creates the destination context and the
     * origin context(s) named in the notice, and stages a PENDING candidate for every Experiment
     * found in those origin context(s) that doesn't already have one. Idempotent: safe to call
     * more than once for the same notice (PNS notices can be redelivered). Returns the
     * destination context's ID, or empty if nothing should be recreated for this notice.
     */
    Optional<Long> stageFromNotice(Claims noticeClaims);

    /**
     * Recreates every PENDING candidate's source Experiment in the given destination context,
     * acting as that Experiment's own instructor (its creator, or failing that any instructor in
     * its course) - or, when actingUserKey is given, as that user instead: an instructor retrying
     * from their own launch into the destination course, e.g. after re-approving LMS access. The
     * source instructor is emailed if the LMS work fails; a retrying instructor isn't, since
     * they're watching the result on the page. Runs with no live session - see ExperimentCopyRecreationAsyncService, which
     * calls this in the background right after stageFromNotice. Each candidate's outcome is
     * recorded on the candidate itself (IMPORTED or ERROR, with an error message) and, once
     * imported, on its resulting ExperimentImport.
     */
    void recreateForContext(long destinationContextId, String actingUserKey);

    /**
     * Puts every failed recreation in the given context back to PENDING - both candidates that
     * failed outright and ones whose import failed - so recreateForContext can try them again.
     * Returns whether there was anything to retry.
     */
    boolean resetFailedForRetry(long contextId);

    /**
     * Finds recreations that stopped part-way - e.g. the server was restarted while they ran -
     * and puts them back to PENDING so they can run again: candidates still PENDING or IMPORTING
     * after stalledAfter, and ones whose import has been processing for longer than
     * importStalledAfter (that import is abandoned). A candidate that has already been started
     * maxAttempts times is marked as failed instead, and its instructor emailed. Returns the
     * destination contexts that have something to recreate again.
     */
    Set<Long> resetStalledForRecovery(Duration stalledAfter, Duration importStalledAfter, int maxAttempts);

    /**
     * Whether recreation is still underway for the given context: any candidate still PENDING or
     * IMPORTING, or IMPORTED with its ExperimentImport still processing. Used to hold off the
     * obsolete-assignment check (NoticeController, ExperimentServiceImpl) until then, since a
     * copied assignment's URL still carries the source course's old IDs until it's re-pointed.
     * A cheap no-op for the overwhelming majority of contexts, which have never been a
     * course-copy destination at all.
     */
    boolean hasUnfinishedForContext(long contextId);

    /**
     * The overall result of recreating copied experiments in the current (live-launch) context,
     * for the first-launch message. NONE once acknowledgeCopyStatus has been called.
     */
    CopyStatusDto getCopyStatus(SecuredInfo securedInfo);

    /**
     * Marks the current context's finished recreation result as shown, along with the
     * ExperimentImports it created, so neither is shown again. Does nothing while still underway.
     */
    void acknowledgeCopyStatus(SecuredInfo securedInfo);

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
