package edu.iu.terracotta.connectors.generic.service.lti;

import java.util.Optional;

import io.jsonwebtoken.Claims;

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

}
