package edu.iu.terracotta.connectors.generic.service.lti;

import java.util.List;
import java.util.Optional;

import io.jsonwebtoken.Claims;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;

/**
 * Resolves the claims of an already-signature-verified LTI Advantage platform notice (e.g. the
 * Platform Notification Service's CourseCopyStarted/CourseCopyCompleted) into a SecuredInfo, so
 * existing SecuredInfo-based services can be driven by a notice instead of a live launch.
 *
 * A notice carries no live user session to build a SecuredInfo from - only the standard LTI
 * deployment_id/context claims identifying which course it concerns - so the "acting" user is
 * chosen from that context's own membership: the first user on record with an instructor (or
 * higher) role. No such user is available (e.g. a course with no prior launch, or one where the
 * platform/deployment/context can't be matched to anything Terracotta already knows about)
 * legitimately returns an empty Optional rather than throwing - the caller should skip the notice
 * in that case.
 */
public interface LtiNoticeService {

    Optional<SecuredInfo> resolveSecuredInfo(Claims noticeClaims);

    /**
     * Resolves the notice's own (destination) context - unlike resolveSecuredInfo, CREATES a
     * minimal LtiContextEntity if none exists yet, since a course-copy notice for a brand new
     * copied course legitimately arrives before anyone has ever launched Terracotta there. NRPS/
     * line-items URLs are left null; a real launch later fills them in via the same upsert path
     * that already tolerates them being null (see LtiDataServiceImpl.upsertLTIDataInDB).
     */
    Optional<LtiContextEntity> resolveOrCreateContext(Claims noticeClaims);

    /**
     * Resolves the LtiContextEntity(s) named in the notice's origin_contexts claim (the course(s)
     * this notice's own context was copied FROM), under the same ToolDeployment as the notice
     * itself. An origin context Terracotta has never seen a launch for is silently omitted, not
     * an error - it legitimately has no Experiments to offer as copy candidates.
     */
    List<LtiContextEntity> resolveOriginContexts(Claims noticeClaims);

}
