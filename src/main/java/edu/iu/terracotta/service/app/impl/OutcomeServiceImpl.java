package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.Outcome;
import edu.iu.terracotta.dao.entity.OutcomeScore;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.OutcomeDto;
import edu.iu.terracotta.dao.model.dto.OutcomePotentialDto;
import edu.iu.terracotta.dao.model.dto.OutcomeScoreDto;
import edu.iu.terracotta.dao.model.enums.LmsType;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureRepository;
import edu.iu.terracotta.dao.repository.OutcomeRepository;
import edu.iu.terracotta.dao.repository.OutcomeScoreRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.OutcomeScoreService;
import edu.iu.terracotta.service.app.OutcomeService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OutcomeServiceImpl implements OutcomeService {

    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ExposureRepository exposureRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private OutcomeRepository outcomeRepository;
    @Autowired private OutcomeScoreRepository outcomeScoreRepository;
    @Autowired private OutcomeScoreService outcomeScoreService;
    @Autowired private ParticipantService participantService;
    @Autowired private ApiClient apiClient;

    @Override
    public List<OutcomeDto> getOutcomesForExposure(long exposureId) {
        return CollectionUtils.emptyIfNull(outcomeRepository.findByExposure_ExposureId(exposureId)).stream()
            .map(outcome -> toDto(outcome, false))
            .toList();
    }

    @Override
    public Outcome getOutcome(long id) {
        return outcomeRepository.findByOutcomeId(id);
    }

    @Override
    public List<OutcomeDto> getAllByExperiment(long experimentId) {
        return CollectionUtils.emptyIfNull(outcomeRepository.findByExposure_Experiment_ExperimentId(experimentId)).stream()
            .map(
                outcome -> toDto(outcome, false)
            )
            .toList();
    }

    @Override
    public OutcomeDto postOutcome(OutcomeDto outcomeDto, long exposureId) throws IdInPostException, DataServiceException, TitleValidationException {
        if (outcomeDto.getOutcomeId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        outcomeDto.setExposureId(exposureId);
        defaultOutcome(outcomeDto);
        Outcome outcome;

        try {
            outcome = fromDto(outcomeDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create Outcome: " + ex.getMessage(), ex);
        }

        return toDto(outcomeRepository.save(outcome), false);
    }

    @Override
    public OutcomeDto toDto(Outcome outcome, boolean outcomeScores) {
        OutcomeDto outcomeDto = new OutcomeDto();
        outcomeDto.setOutcomeId(outcome.getOutcomeId());
        outcomeDto.setExposureId(outcome.getExposure().getExposureId());
        outcomeDto.setTitle(outcome.getTitle());
        outcomeDto.setLmsType(outcome.getLmsType().name());
        outcomeDto.setLmsOutcomeId(outcome.getLmsOutcomeId());
        outcomeDto.setMaxPoints(outcome.getMaxPoints());
        outcomeDto.setExternal(outcome.getExternal());
        List<OutcomeScoreDto> outcomeScoreDtoList = new ArrayList<>();

        if (outcomeScores) {
            List<OutcomeScore> outcomeScoreList = outcomeScoreRepository.findByOutcome_OutcomeId(outcome.getOutcomeId());

            for (OutcomeScore outcomeScore : outcomeScoreList) {
                outcomeScoreDtoList.add(outcomeScoreService.toDto(outcomeScore));
            }
        }

        outcomeDto.setOutcomeScoreDtoList(outcomeScoreDtoList);

        return outcomeDto;
    }

    @Override
    public Outcome fromDto(OutcomeDto outcomeDto) throws DataServiceException {
        Optional<Exposure> exposure = exposureRepository.findById(outcomeDto.getExposureId());

        if (exposure.isEmpty()) {
            throw new DataServiceException("Exposure for outcome does not exist.");
        }

        Outcome outcome = new Outcome();
        outcome.setOutcomeId(outcomeDto.getOutcomeId());
        outcome.setTitle(outcomeDto.getTitle());
        outcome.setLmsType(EnumUtils.getEnum(LmsType.class, outcomeDto.getLmsType(), LmsType.none));
        outcome.setMaxPoints(outcomeDto.getMaxPoints());
        outcome.setLmsOutcomeId(outcomeDto.getLmsOutcomeId());
        outcome.setExternal(outcomeDto.getExternal());
        outcome.setExposure(exposure.get());

        return outcome;
    }

    private Optional<Outcome> findById(long id) {
        return outcomeRepository.findById(id);
    }

    @Override
    public void updateOutcome(long outcomeId, OutcomeDto outcomeDto) throws TitleValidationException {
        Outcome outcome = outcomeRepository.findByOutcomeId(outcomeId);

        if (StringUtils.isAllBlank(outcomeDto.getTitle(), outcome.getTitle())) {
            throw new TitleValidationException("Error 100: Please give the outcome a title.");
        }

        if (StringUtils.isNotBlank(outcomeDto.getTitle()) && outcomeDto.getTitle().length() > 255) {
            throw new TitleValidationException("Error 101: The title must be 255 characters or less.");
        }

        // only allow external to be changed if the current value is null. (Only allow it to be changed once)
        if (outcome.getExternal() == null && outcomeDto.getExternal() != null) {
            outcome.setExternal(outcomeDto.getExternal());

            if (!outcomeDto.getExternal()) {
                outcome.setLmsOutcomeId(null);
                outcome.setLmsType(EnumUtils.getEnum(LmsType.class, LmsType.none.name()));
            } else {
                outcome.setLmsOutcomeId(outcomeDto.getLmsOutcomeId());
                outcome.setLmsType(EnumUtils.getEnum(LmsType.class, outcomeDto.getLmsType()));
            }
        }

        outcome.setTitle(outcomeDto.getTitle());
        outcome.setMaxPoints(outcomeDto.getMaxPoints());

        outcomeRepository.saveAndFlush(outcome);
    }

    @Override
    public void deleteById(long id) {
        outcomeRepository.deleteByOutcomeId(id);
    }

    @Override
    public List<OutcomePotentialDto> potentialOutcomes(long experimentId, SecuredInfo securedInfo) throws DataServiceException, ApiException, TerracottaConnectorException {
        Optional<Experiment> experiment = experimentRepository.findById(experimentId);

        if (experiment.isEmpty()) {
            throw new DataServiceException("Error 105: Experiment does not exist.");
        }

        LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        List<Assignment> assignmentList = assignmentRepository.findByExposure_Experiment_ExperimentId(experimentId);
        List<OutcomePotentialDto> outcomePotentialDtos = new ArrayList<>();
        String lmsCourseId = StringUtils.substringBetween(experiment.get().getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");
        List<? extends LmsAssignment> lmsAssignments = apiClient.listAssignments(instructorUser, lmsCourseId);

        for (LmsAssignment lmsAssignment : lmsAssignments) {
            List<Assignment> matched = assignmentList.stream()
                .filter(x -> {
                    return StringUtils.equalsIgnoreCase(lmsAssignment.getId(), x.getLmsAssignmentId());
                })
                .toList();

            if (matched.isEmpty()
                    && !(experiment.get().getConsentDocument() != null
                    && StringUtils.equalsIgnoreCase(lmsAssignment.getId(), experiment.get().getConsentDocument().getLmsAssignmentId()))) {
                outcomePotentialDtos.add(lmsAssignmentToOutcomePotentialDto(lmsAssignment));
            }
        }

        return outcomePotentialDtos;
    }

    private OutcomePotentialDto lmsAssignmentToOutcomePotentialDto(LmsAssignment lmsAssignment) {
        OutcomePotentialDto potentialDto = new OutcomePotentialDto();
        potentialDto.setAssignmentId(lmsAssignment.getId());
        potentialDto.setName(lmsAssignment.getName());
        potentialDto.setType(lmsAssignment.getSubmissionTypes().get(0));
        potentialDto.setPointsPossible(lmsAssignment.getPointsPossible());
        potentialDto.setTerracotta(lmsAssignment.getSubmissionTypes().contains("external_tool"));

        return potentialDto;
    }

    @Override
    @Transactional
    public void updateOutcomeGrades(long outcomeId, SecuredInfo securedInfo, boolean refreshParticipants) throws ApiException, IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, NumberFormatException, TerracottaConnectorException {
        Optional<Outcome> outcomeSearchResult = this.findById(outcomeId);

        if (outcomeSearchResult.isEmpty()) {
            throw new OutcomeNotMatchingException(TextConstants.OUTCOME_NOT_MATCHING);
        }

        Outcome outcome = outcomeSearchResult.get();

        if (BooleanUtils.isNotTrue(outcome.getExternal())) {
            // this outcome is not external; don't need to check the scores
            return;
        }

        if (refreshParticipants) {
            participantService.refreshParticipants(outcome.getExposure().getExperiment().getExperimentId(), outcome.getExposure().getExperiment().getParticipants());
        }

        List<OutcomeScore> newScores = new ArrayList<>();
        String lmsCourseId = StringUtils.substringBetween(outcome.getExposure().getExperiment().getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");
        LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        List<LmsSubmission> submissions = apiClient.listSubmissions(instructorUser, outcome.getLmsOutcomeId(), lmsCourseId);

        for (LmsSubmission lmsSubmission : submissions) {
            boolean found = false;

            for (OutcomeScore outcomeScore : outcome.getOutcomeScores()) {
                if (outcomeScore.getParticipant().isTestStudent()) {
                    continue;
                }

                if (outcomeScore.getParticipant().getLtiUserEntity().getEmail() != null &&
                    StringUtils.equals(outcomeScore.getParticipant().getLtiUserEntity().getEmail(), lmsSubmission.getUserLoginId()) &&
                    StringUtils.equals(outcomeScore.getParticipant().getLtiUserEntity().getDisplayName(), lmsSubmission.getUserName())
                ) {
                    found = true;

                    if (lmsSubmission.getScore() == null) {
                        outcomeScore.setScoreNumeric(null);
                    } else {
                        outcomeScore.setScoreNumeric(lmsSubmission.getScore().floatValue());
                    }

                    break;
                }
            }

            if (!found) {
                for (OutcomeScore outcomeScore : outcome.getOutcomeScores()) {
                    if (outcomeScore.getParticipant().isTestStudent()) {
                        continue;
                    }

                    if (StringUtils.equals(outcomeScore.getParticipant().getLtiUserEntity().getDisplayName(), lmsSubmission.getUserName())) {
                        found = true;

                        if (lmsSubmission.getScore() == null) {
                            outcomeScore.setScoreNumeric(null);
                        } else {
                            outcomeScore.setScoreNumeric(lmsSubmission.getScore().floatValue());
                        }

                        break;
                    }
                }
            }

            if (!found) {
                for (Participant participant : outcome.getExposure().getExperiment().getParticipants()) {
                    if (participant.isTestStudent()) {
                        continue;
                    }

                    if (participant.getLtiUserEntity().getEmail() != null &&
                        StringUtils.equals(participant.getLtiUserEntity().getEmail(), lmsSubmission.getUserLoginId()) &&
                        StringUtils.equals(participant.getLtiUserEntity().getDisplayName(), lmsSubmission.getUserName())
                    ) {
                        found = true;
                        OutcomeScore outcomeScore = new OutcomeScore();
                        outcomeScore.setOutcome(outcome);
                        outcomeScore.setParticipant(participant);

                        if (lmsSubmission.getScore() == null) {
                            outcomeScore.setScoreNumeric(null);
                        } else {
                            outcomeScore.setScoreNumeric(lmsSubmission.getScore().floatValue());
                        }

                        newScores.add(outcomeScore);
                        break;
                    }
                }
            }

            if (!found) {
                for (Participant participant : outcome.getExposure().getExperiment().getParticipants()) {
                    if (participant.isTestStudent()) {
                        continue;
                    }

                    if (participant.getLtiUserEntity().getDisplayName().equals(lmsSubmission.getUserName())) {
                        OutcomeScore outcomeScore = new OutcomeScore();
                        outcomeScore.setOutcome(outcome);
                        outcomeScore.setParticipant(participant);

                        if (lmsSubmission.getScore() == null) {
                            outcomeScore.setScoreNumeric(null);
                        } else {
                            outcomeScore.setScoreNumeric(lmsSubmission.getScore().floatValue());
                        }

                        newScores.add(outcomeScore);
                        break;
                    }
                }
            }
        }

        newScores.forEach(
            outcomeScore -> outcomeScoreRepository.save(outcomeScore)
        );

        // TODO what to do if the outcome score is there but the participant is dropped.
    }

    @Override
    public void defaultOutcome(OutcomeDto outcomeDto) throws TitleValidationException {
        if (StringUtils.isNotBlank(outcomeDto.getTitle()) && outcomeDto.getTitle().length() > 255) {
            throw new TitleValidationException("Error 101: The title must be 255 characters or less.");
        }

        if (BooleanUtils.isFalse(outcomeDto.getExternal())) {
            outcomeDto.setLmsOutcomeId(null);
            outcomeDto.setLmsType("NONE");
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long exposureId, long outcomeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/exposures/{exposureId}/outcomes/{outcomeId}")
                .buildAndExpand(experimentId, exposureId, outcomeId).toUri());

        return headers;
    }

}
