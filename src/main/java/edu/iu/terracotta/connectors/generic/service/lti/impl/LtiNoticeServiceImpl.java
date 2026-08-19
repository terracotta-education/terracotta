package edu.iu.terracotta.connectors.generic.service.lti.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
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

    private final ToolDeploymentRepository toolDeploymentRepository;
    private final LtiContextRepository ltiContextRepository;
    private final LtiMembershipRepository ltiMembershipRepository;

    @Override
    public Optional<SecuredInfo> resolveSecuredInfo(Claims noticeClaims) {
        String iss = noticeClaims.getIssuer();
        String clientId = Optional.ofNullable(noticeClaims.getAudience()).stream()
            .flatMap(Set::stream)
            .findFirst()
            .orElse(null);
        String deploymentId = noticeClaims.get(LtiStrings.LTI_DEPLOYMENT_ID, String.class);
        String contextKey = getContextKey(noticeClaims);

        if (StringUtils.isAnyBlank(iss, clientId, deploymentId, contextKey)) {
            log.warn(
                "Notice is missing one or more claims needed to resolve a context - iss: [{}], clientId: [{}], deploymentId: [{}], contextKey: [{}]",
                iss,
                clientId,
                deploymentId,
                contextKey
            );

            return Optional.empty();
        }

        return toolDeploymentRepository.findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId(iss, clientId, deploymentId).stream()
            .findFirst()
            .map(toolDeployment -> ltiContextRepository.findByContextKeyAndToolDeployment(contextKey, toolDeployment))
            .flatMap(this::resolveActingInstructor);
    }

    private String getContextKey(Claims noticeClaims) {
        Object context = noticeClaims.get(LtiStrings.LTI_CONTEXT);

        if (!(context instanceof Map<?, ?> contextMap)) {
            return null;
        }

        Object contextId = contextMap.get(LtiStrings.LTI_CONTEXT_ID);

        return contextId instanceof String ? (String) contextId : null;
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
