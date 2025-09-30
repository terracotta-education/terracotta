package edu.iu.terracotta.service.app.async.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetUsersInCourseOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentState;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentType;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUtils;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.async.ParticipantAsyncService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ParticipantAsyncServiceImpl implements ParticipantAsyncService {

    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private ApiClient apiClient;
    @Autowired private FeatureService featureService;
    @Autowired private LmsUtils lmsUtils;

    @Async
    @Override
    @Transactional(rollbackFor = { ApiException.class })
    public void updateParticipantData(long experimentId, SecuredInfo securedInfo) throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);

        if (experiment == null) {
            log.error("Experiment not found for ID: [{}]", experimentId);
            return;
        }

        if (!featureService.isFeatureEnabled(FeatureType.MESSAGING, experiment.getPlatformDeployment().getKeyId())) {
            log.info(
                "Messaging is not enabled for the platform deployment with key ID: [{}]. Participant data update aborted for experiment ID: [{}].",
                experiment.getPlatformDeployment().getKeyId(),
                experimentId
            );

            return;
        }

        List<Participant> participants = participantRepository.findByExperiment_ExperimentId(experimentId);

        if (CollectionUtils.isEmpty(participants)) {
            log.info("No participants found for the experiment with ID: [{}]", experimentId);
            return;
        }

        List<LmsUser> students = apiClient
            .listUsersForCourse(
                LmsGetUsersInCourseOptions.builder()
                    .lmsCourseId(lmsUtils.parseCourseId(experiment.getPlatformDeployment(), experiment.getLtiContextEntity().getContext_memberships_url()))
                    .enrollmentState(Arrays.asList(EnrollmentState.ACTIVE, EnrollmentState.INVITED))
                    .enrollmentType(Arrays.asList(EnrollmentType.STUDENT))
                    .build(),
                experiment.getCreatedBy()
            );

        if (CollectionUtils.isEmpty(students)) {
            log.info(
                "No students found for the course with ID: [{}] in the platform deployment with key ID: [{}]",
                experiment.getLtiContextEntity().getContext_memberships_url(),
                experiment.getPlatformDeployment().getKeyId()
            );
            return;
        }

        // Update participants without LTI user IDs based on email matching
        participants.stream()
            .filter(participant -> participant.getLtiUserEntity().getLmsUserId() == null)
            .forEach(
                participant -> {
                    participant.getLtiUserEntity().setLmsUserId(
                        students.stream()
                            .filter(student -> Strings.CI.equals(student.getEmail(), participant.getLtiUserEntity().getEmail()))
                            .findFirst()
                            .map(LmsUser::getId)
                            .orElse(null)
                    );

                    participantRepository.save(participant);
                }
            );
    }

}
