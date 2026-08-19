package edu.iu.terracotta.service.app.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.service.app.ParticipantRosterWriteService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.ParticipantConsentUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ParticipantRosterWriteServiceImpl implements ParticipantRosterWriteService {

    private final ParticipantRepository participantRepository;
    private final LtiDataService ltiDataService;

    @PersistenceContext private EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncParticipantsPage(Experiment experiment, List<LmsUserBatch> batchUsers) {
        // the LMS occasionally returns a roster entry with no usable user identifier (e.g. a
        // pending/placeholder enrollment) - LtiUserEntity's constructor asserts on a blank
        // userKey, and letting one through would blow up this whole page's transaction, silently
        // dropping every other participant in the page along with it. One summary line rather
        // than one per row, since a course missing this field tends to be missing it for every row.
        long skipped = batchUsers.stream().filter(batchUser -> StringUtils.isBlank(batchUser.getUserKey())).count();

        if (skipped > 0) {
            log.warn("Skipped [{}] LMS user batch row(s) with a blank user key for experiment ID: [{}]", skipped, experiment.getExperimentId());
            batchUsers = batchUsers.stream().filter(batchUser -> StringUtils.isNotBlank(batchUser.getUserKey())).toList();
        }

        // retrieve batch of participants; reset dropped value and will set to false later, if found in the course roster
        List<Participant> participants = participantRepository.findAllByExperiment_ExperimentIdAndLtiUserEntity_UserKeyIn(
            experiment.getExperimentId(),
            batchUsers.stream()
                .map(LmsUserBatch::getUserKey)
                .toList()
        )
        .stream()
        .map(
            p -> {
                p.setDropped(true);
                return p;
            }
        )
        .toList();

        // batch the LtiUserEntity (and, for ones that turn out to already exist, LtiMembershipEntity)
        // lookup for every batchUser not already a participant here, instead of one query per new
        // user - significant for a huge course's first sync, where most users are new.
        List<String> userKeysNeedingLtiUserLookup = batchUsers.stream()
            .map(LmsUserBatch::getUserKey)
            .filter(userKey -> participants.stream().noneMatch(p -> Strings.CI.equals(p.getLtiUserEntity().getUserKey(), userKey)))
            .toList();

        List<LtiUserEntity> ltiUserEntities = userKeysNeedingLtiUserLookup.isEmpty()
            ? List.of()
            : ltiDataService.findAllByUserKeysAndPlatformDeployment(userKeysNeedingLtiUserLookup, experiment.getPlatformDeployment());

        Map<Long, LtiMembershipEntity> membershipsByLtiUserId = ltiUserEntities.isEmpty()
            ? Map.of()
            : ltiDataService.findAllByUsersAndContext(ltiUserEntities, experiment.getLtiContextEntity())
                .stream()
                .collect(Collectors.toMap(m -> m.getUser().getUserId(), Function.identity(), (a, b) -> a));

        for (LmsUserBatch batchUser : batchUsers) {
            Participant participant = participants.stream()
                .filter(p -> Strings.CI.equals(p.getLtiUserEntity().getUserKey(), batchUser.getUserKey()))
                .findFirst()
                .orElseGet(() -> createNewParticipant(batchUser, experiment, ltiUserEntities, membershipsByLtiUserId));

            ParticipantConsentUtils.resetConsentIfExperimentNotStarted(experiment, participant);

            participant.setDropped(false);
            participantRepository.save(participant);
        }

        entityManager.flush();
        entityManager.clear();
    }

    private Participant createNewParticipant(LmsUserBatch batchUser, Experiment experiment, List<LtiUserEntity> ltiUserEntities, Map<Long, LtiMembershipEntity> membershipsByLtiUserId) {
        LtiUserEntity ltiUserEntity = ltiUserEntities.stream()
            .filter(u -> Strings.CI.equals(u.getUserKey(), batchUser.getUserKey()))
            .findFirst()
            .orElse(null);

        if (ltiUserEntity == null) {
            LtiUserEntity newLtiUserEntity = new LtiUserEntity(batchUser.getUserKey(), null, experiment.getPlatformDeployment());
            newLtiUserEntity.setEmail(batchUser.getEmail());
            /*
                TODO: We don't have a way here to get the userLmsId except calling the API
                or waiting for the user to access. BUT we just need this to send the grades with the API...
                so if the user never accessed... we can't send them until we use LTI.
            */
            newLtiUserEntity.setDisplayName(batchUser.getName());
            // By default it adds a value in the constructor, but if we are generating it, it means that the user has never logged in
            newLtiUserEntity.setLoginAt(null);
            ltiUserEntity = ltiDataService.saveLtiUserEntity(newLtiUserEntity);
        }

        return buildAndSaveParticipant(ltiUserEntity, experiment, membershipsByLtiUserId);
    }

    private Participant buildAndSaveParticipant(LtiUserEntity ltiUserEntity, Experiment experiment, Map<Long, LtiMembershipEntity> membershipsByLtiUserId) {
        LtiMembershipEntity ltiMembershipEntity = membershipsByLtiUserId.get(ltiUserEntity.getUserId());

        if (ltiMembershipEntity == null) {
            ltiMembershipEntity = ltiDataService.findByUserAndContext(ltiUserEntity, experiment.getLtiContextEntity());
        }

        if (ltiMembershipEntity == null) {
            ltiMembershipEntity = new LtiMembershipEntity(experiment.getLtiContextEntity(), ltiUserEntity, LtiStrings.ROLE_STUDENT);
            ltiMembershipEntity = ltiDataService.saveLtiMembershipEntity(ltiMembershipEntity);
        }

        Participant newParticipant = Participant.builder()
            .experiment(experiment)
            .dropped(false)
            .ltiUserEntity(ltiUserEntity)
            .ltiMembershipEntity(ltiMembershipEntity)
            .source(experiment.getParticipationType())
            .build();

        switch (experiment.getParticipationType()) {
            case MANUAL:
                newParticipant.setConsent(null);
                break;
            case CONSENT:
                newParticipant.setConsent(false);
                break;
            case AUTO:
                newParticipant.setConsent(true);
                newParticipant.setDateGiven(Timestamp.from(Instant.now()));
                break;
            default:
        }

        return participantRepository.save(newParticipant);
    }

}
