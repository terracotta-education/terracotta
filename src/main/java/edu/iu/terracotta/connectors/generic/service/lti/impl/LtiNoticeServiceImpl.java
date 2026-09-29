package edu.iu.terracotta.connectors.generic.service.lti.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lti.Roles;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiMembershipRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.ToolDeploymentRepository;
import edu.iu.terracotta.connectors.generic.service.lti.LtiNoticeService;
import edu.iu.terracotta.utils.LtiStrings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class LtiNoticeServiceImpl implements LtiNoticeService {

    // role is an ordinal scale (see Lti3Request.makeUserRoleNum): 0 = general/learner,
    // 1 = instructor, 2 = admin
    private static final int INSTRUCTOR_ROLE = 1;
    private static final String UNKNOWN = "unknown";

    private final ToolDeploymentRepository toolDeploymentRepository;
    private final LtiContextRepository ltiContextRepository;
    private final LtiMembershipRepository ltiMembershipRepository;

    @Override
    public Optional<SecuredInfo> resolveSecuredInfo(Claims noticeClaims) {
        String contextKey = getContextKey(noticeClaims);

        if (StringUtils.isBlank(contextKey)) {
            log.warn("Notice is missing the context claim needed to resolve a context. Issuer: [{}]", noticeClaims.getIssuer());

            return Optional.empty();
        }

        return resolveToolDeployment(noticeClaims)
            .map(toolDeployment -> ltiContextRepository.findByContextKeyAndToolDeployment(contextKey, toolDeployment))
            .flatMap(this::resolveActingInstructor);
    }

    @Override
    public Optional<LtiContextEntity> resolveOrCreateContext(Claims noticeClaims) {
        String contextKey = getContextKey(noticeClaims);

        if (StringUtils.isBlank(contextKey)) {
            log.warn("Notice is missing the context claim needed to resolve a context. Issuer: [{}]", noticeClaims.getIssuer());

            return Optional.empty();
        }

        return resolveToolDeployment(noticeClaims)
            .map(toolDeployment -> {
                LtiContextEntity existing = ltiContextRepository.findByContextKeyAndToolDeployment(contextKey, toolDeployment);

                if (existing != null) {
                    return existing;
                }

                // brand-new copied course - nobody has launched Terracotta there yet, so there's
                // no LtiContextEntity to find. NRPS/line-items URLs are left null, matching the
                // same nullable fields a live launch itself leaves unset until upsertLTIDataInDB
                // fills them in (see LtiDataServiceImpl) - a real launch into this course later
                // updates this same row rather than creating a duplicate.
                return ltiContextRepository.save(
                    new LtiContextEntity(contextKey, toolDeployment, getContextTitle(noticeClaims), null, null, null)
                );
            });
    }

    @Override
    public List<LtiContextEntity> resolveOriginContexts(Claims noticeClaims) {
        return resolveToolDeployment(noticeClaims)
            .map(toolDeployment ->
                getOriginContextKeys(noticeClaims).stream()
                    .map(originContextKey -> ltiContextRepository.findByContextKeyAndToolDeployment(originContextKey, toolDeployment))
                    .filter(Objects::nonNull)
                    .toList()
            )
            .orElse(List.of());
    }

    @Override
    public CourseCopyNoticeDescription describeCourseCopy(Claims noticeClaims) {
        Optional<ToolDeployment> toolDeployment = resolveToolDeployment(noticeClaims);

        String platform = getPlatformUrl(noticeClaims)
            .or(() -> toolDeployment.map(ToolDeployment::getPlatformDeployment).map(PlatformDeployment::getBaseUrl).filter(StringUtils::isNotBlank))
            .orElse(StringUtils.defaultIfBlank(noticeClaims.getIssuer(), UNKNOWN));

        String source = getOriginContextKeys(noticeClaims).stream()
            .map(originContextKey -> describeContext(
                toolDeployment
                    .map(deployment -> ltiContextRepository.findByContextKeyAndToolDeployment(originContextKey, deployment))
                    .map(LtiContextEntity::getTitle)
                    .orElse(null),
                originContextKey
            ))
            .collect(Collectors.joining(", "));

        return new CourseCopyNoticeDescription(
            platform,
            StringUtils.defaultIfBlank(source, UNKNOWN),
            describeContext(getContextTitle(noticeClaims), getContextKey(noticeClaims))
        );
    }

    private Optional<String> getPlatformUrl(Claims noticeClaims) {
        if (!(noticeClaims.get(LtiStrings.LTI_PLATFORM) instanceof Map<?, ?> platform)) {
            return Optional.empty();
        }

        return platform.get(LtiStrings.LTI_PLATFORM_URL) instanceof String url && StringUtils.isNotBlank(url)
            ? Optional.of(url)
            : Optional.empty();
    }

    private String describeContext(String title, String contextKey) {
        if (StringUtils.isBlank(contextKey)) {
            return StringUtils.defaultIfBlank(title, UNKNOWN);
        }

        return StringUtils.isBlank(title) ? contextKey : String.format("%s (%s)", title, contextKey);
    }

    private Optional<ToolDeployment> resolveToolDeployment(Claims noticeClaims) {
        String iss = noticeClaims.getIssuer();
        String clientId = Optional.ofNullable(noticeClaims.getAudience()).stream()
            .flatMap(Set::stream)
            .findFirst()
            .orElse(null);
        String deploymentId = noticeClaims.get(LtiStrings.LTI_DEPLOYMENT_ID, String.class);

        if (StringUtils.isAnyBlank(iss, clientId, deploymentId)) {
            log.warn(
                "Notice is missing one or more claims needed to resolve a tool deployment - iss: [{}], clientId: [{}], deploymentId: [{}]",
                iss,
                clientId,
                deploymentId
            );

            return Optional.empty();
        }

        return toolDeploymentRepository.findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId(iss, clientId, deploymentId).stream()
            .findFirst();
    }

    private String getContextKey(Claims noticeClaims) {
        Object contextId = getContextClaim(noticeClaims, LtiStrings.LTI_CONTEXT_ID);

        return contextId instanceof String ? (String) contextId : null;
    }

    private String getContextTitle(Claims noticeClaims) {
        Object title = getContextClaim(noticeClaims, LtiStrings.LTI_CONTEXT_TITLE);

        return title instanceof String contextTitle ? contextTitle : null;
    }

    private Object getContextClaim(Claims noticeClaims, String key) {
        Object context = noticeClaims.get(LtiStrings.LTI_CONTEXT);

        if (!(context instanceof Map<?, ?> contextMap)) {
            return null;
        }

        return contextMap.get(key);
    }

    private List<String> getOriginContextKeys(Claims noticeClaims) {
        Object originContexts = noticeClaims.get(LtiStrings.LTI_ORIGIN_CONTEXTS);

        if (!(originContexts instanceof Collection<?> originContextsCollection)) {
            return List.of();
        }

        return originContextsCollection.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .toList();
    }

    private Optional<SecuredInfo> resolveActingInstructor(LtiContextEntity ltiContextEntity) {
        if (ltiContextEntity == null) {
            return Optional.empty();
        }

        return ltiMembershipRepository.findFirstByContextAndRoleGreaterThanEqual(ltiContextEntity, INSTRUCTOR_ROLE)
            .map(LtiMembershipEntity::getUser)
            .map(
                actingUser -> {
                    ToolDeployment toolDeployment = ltiContextEntity.getToolDeployment();

                    return SecuredInfo.builder()
                        .platformDeploymentId(toolDeployment.getPlatformDeployment().getKeyId())
                        .contextId(ltiContextEntity.getContextId())
                        .userId(actingUser.getUserKey())
                        .roles(Roles.INSTRUCTOR_ROLE_LIST)
                        .lmsCourseId(ltiContextEntity.getContextKey())
                        .build();
                }
            );
    }

}
