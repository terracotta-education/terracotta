package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.OutcomeDto;
import edu.iu.terracotta.model.app.dto.OutcomePotentialDto;
import edu.iu.terracotta.model.app.dto.OutcomeScoreDto;
import edu.iu.terracotta.model.app.enumerator.LmsType;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.OutcomeScoreService;
import edu.iu.terracotta.service.app.OutcomeService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.utils.TextConstants;
import edu.ksu.canvas.model.assignment.Submission;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private OutcomeScoreService outcomeScoreService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private ExposureService exposureService;

    @Autowired
    private ExperimentService experimentService;

    @Autowired
    private CanvasAPIClient canvasAPIClient;

    @Override
    public List<Outcome> findAllByExposureId(Long exposureId) {
        return allRepositories.outcomeRepository.findByExposure_ExposureId(exposureId);
    }

    @Override
    public List<Outcome> findAllByExperiment(long experimentId) {
        return allRepositories.outcomeRepository.findByExposure_Experiment_ExperimentId(experimentId);
    }

    @Override
    public List<Outcome> findAllByExperiment(long experimentId, Pageable pageable) {
        return allRepositories.outcomeRepository.findByExposure_Experiment_ExperimentId(experimentId, pageable).getContent();
    }

    @Override
    public List<OutcomeDto> getOutcomes(Long exposureId) {
        return CollectionUtils.emptyIfNull(findAllByExposureId(exposureId)).stream().map(outcome -> toDto(outcome, false)).toList();
    }

    @Override
    public Outcome getOutcome(Long id) {
        return allRepositories.outcomeRepository.findByOutcomeId(id);
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

        return toDto(save(outcome), false);
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
            List<OutcomeScore> outcomeScoreList = allRepositories.outcomeScoreRepository.findByOutcome_OutcomeId(outcome.getOutcomeId());

            for (OutcomeScore outcomeScore : outcomeScoreList) {
                outcomeScoreDtoList.add(outcomeScoreService.toDto(outcomeScore));
            }
        }

        outcomeDto.setOutcomeScoreDtoList(outcomeScoreDtoList);

        return outcomeDto;
    }

    @Override
    public Outcome fromDto(OutcomeDto outcomeDto) throws DataServiceException {
        Outcome outcome = new Outcome();
        outcome.setOutcomeId(outcomeDto.getOutcomeId());
        outcome.setTitle(outcomeDto.getTitle());
        outcome.setLmsType(EnumUtils.getEnum(LmsType.class, outcomeDto.getLmsType(), LmsType.none));
        outcome.setMaxPoints(outcomeDto.getMaxPoints());
        outcome.setLmsOutcomeId(outcomeDto.getLmsOutcomeId());
        outcome.setExternal(outcomeDto.getExternal());
        Optional<Exposure> exposure = exposureService.findById(outcomeDto.getExposureId());

        if (!exposure.isPresent()) {
            throw new DataServiceException("Exposure for outcome does not exist.");
        }

        outcome.setExposure(exposure.get());

        return outcome;
    }

    @Override
    public Outcome save(Outcome outcome) {
        return allRepositories.outcomeRepository.save(outcome);
    }

    @Override
    public Optional<Outcome> findById(Long id) {
        return allRepositories.outcomeRepository.findById(id);
    }

    @Override
    public void updateOutcome(Long outcomeId, OutcomeDto outcomeDto) throws TitleValidationException {
        Outcome outcome = getOutcome(outcomeId);

        if (StringUtils.isAllBlank(outcomeDto.getTitle(), outcome.getTitle())) {
            throw new TitleValidationException("Error 100: Please give the outcome a title.");
        }

        if (StringUtils.isNotBlank(outcomeDto.getTitle()) && outcomeDto.getTitle().length() > 255) {
            throw new TitleValidationException("Error 101: The title must be 255 characters or less.");
        }

        //only allow external to be changed if the current value is null. (Only allow it to be changed once)
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

        saveAndFlush(outcome);
    }

    @Override
    public void saveAndFlush(Outcome outcomeToChange) {
        allRepositories.outcomeRepository.saveAndFlush(outcomeToChange);
    }

    @Override
    public void deleteById(Long id) {
        allRepositories.outcomeRepository.deleteByOutcomeId(id);
    }

    @Override
    public boolean outcomeBelongsToExperimentAndExposure(Long experimentId, Long exposureId, Long outcomeId) {
        return allRepositories.outcomeRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOutcomeId(experimentId, exposureId, outcomeId);
    }

    @Override
    public List<OutcomePotentialDto> potentialOutcomes(Long experimentId, SecuredInfo securedInfo) throws DataServiceException, CanvasApiException {
        Optional<Experiment> experiment = experimentService.findById(experimentId);
        LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        List<Assignment> assignmentList = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentId(experimentId);
        List<OutcomePotentialDto> outcomePotentialDtos = new ArrayList<>();

        if (!experiment.isPresent()) {
            throw new DataServiceException("Error 105: Experiment does not exist.");
        }

        String canvasCourseId = StringUtils.substringBetween(experiment.get().getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");
        List<AssignmentExtended> assignmentExtendedList = canvasAPIClient.listAssignments(instructorUser, canvasCourseId);

        for (AssignmentExtended assignmentExtended : assignmentExtendedList) {
            List<Assignment> matched = assignmentList.stream()
                .filter(x -> {
                    return assignmentExtended.getId().intValue() == Integer.parseInt(x.getLmsAssignmentId());
                })
                .toList();

            if (matched.isEmpty()
                    && !(experiment.get().getConsentDocument() != null
                    && assignmentExtended.getId().intValue() == Integer.parseInt(experiment.get().getConsentDocument().getLmsAssignmentId()))) {
                outcomePotentialDtos.add(assignmentExtendedToOutcomePotentialDto(assignmentExtended, securedInfo));
            }
        }

        return outcomePotentialDtos;
    }

    private OutcomePotentialDto assignmentExtendedToOutcomePotentialDto(AssignmentExtended assignmentExtended, SecuredInfo securedInfo) {
        OutcomePotentialDto potentialDto = new OutcomePotentialDto();
        potentialDto.setAssignmentId(assignmentExtended.getId());
        potentialDto.setName(assignmentExtended.getName());
        potentialDto.setType(assignmentExtended.getSubmissionTypes().get(0));
        potentialDto.setPointsPossible(assignmentExtended.getPointsPossible());

        Optional<PlatformDeployment> platformDeployment = allRepositories.platformDeploymentRepository.findById(securedInfo.getPlatformDeploymentId());
        potentialDto.setTerracotta(assignmentExtended.getSubmissionTypes().contains("external_tool") && assignmentExtended.getExternalToolTagAttributes().getUrl().contains(platformDeployment.get().getLocalUrl()));

        return potentialDto;
    }

    @Override
    @Transactional
    public void updateOutcomeGrades(Long outcomeId, SecuredInfo securedInfo) throws CanvasApiException, IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        Optional<Outcome> outcomeSearchResult = this.findById(outcomeId);

        if (!outcomeSearchResult.isPresent()) {
            throw new OutcomeNotMatchingException(TextConstants.OUTCOME_NOT_MATCHING);
        }

        Outcome outcome = outcomeSearchResult.get();
        //If this is not external we don't need to check the scores.

        if (BooleanUtils.isNotTrue(outcome.getExternal())) {
            return;
        }

        participantService.refreshParticipants(outcome.getExposure().getExperiment().getExperimentId(), outcome.getExposure().getExperiment().getParticipants());
        List<OutcomeScore> newScores = new ArrayList<>();
        String canvasCourseId = StringUtils.substringBetween(outcome.getExposure().getExperiment().getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");
        LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        List<Submission> submissions = canvasAPIClient.listSubmissions(instructorUser, Integer.parseInt(outcome.getLmsOutcomeId()), canvasCourseId);

        for (Submission submission : submissions) {
            boolean found = false;

            for (OutcomeScore outcomeScore : outcome.getOutcomeScores()) {
                if (outcomeScore.getParticipant().getLtiUserEntity().getEmail() != null && outcomeScore.getParticipant().getLtiUserEntity().getEmail().equals(submission.getUser().getLoginId()) && outcomeScore.getParticipant().getLtiUserEntity().getDisplayName().equals(submission.getUser().getName())) {
                    found = true;

                    if (submission.getScore() == null) {
                        outcomeScore.setScoreNumeric(null);
                    } else {
                        outcomeScore.setScoreNumeric(submission.getScore().floatValue());
                    }

                    break;
                }
            }

            if (!found) {
                for (OutcomeScore outcomeScore : outcome.getOutcomeScores()) {
                    if (outcomeScore.getParticipant().getLtiUserEntity().getDisplayName().equals(submission.getUser().getName())) {
                        found = true;

                        if (submission.getScore() == null) {
                            outcomeScore.setScoreNumeric(null);
                        } else {
                            outcomeScore.setScoreNumeric(submission.getScore().floatValue());
                        }

                        break;
                    }
                }
            }

            if (!found) {
                for (Participant participant : outcome.getExposure().getExperiment().getParticipants()) {
                    if (participant.getLtiUserEntity().getEmail() != null && participant.getLtiUserEntity().getEmail().equals(submission.getUser().getLoginId()) && participant.getLtiUserEntity().getDisplayName().equals(submission.getUser().getName())) {
                        found = true;
                        OutcomeScore outcomeScore = new OutcomeScore();
                        outcomeScore.setOutcome(outcome);
                        outcomeScore.setParticipant(participant);

                        if (submission.getScore() == null) {
                            outcomeScore.setScoreNumeric(null);
                        } else {
                            outcomeScore.setScoreNumeric(submission.getScore().floatValue());
                        }

                        newScores.add(outcomeScore);
                        break;
                    }
                }
            }

            if (!found) {
                for (Participant participant : outcome.getExposure().getExperiment().getParticipants()) {
                    if (participant.getLtiUserEntity().getDisplayName().equals(submission.getUser().getName())) {
                        OutcomeScore outcomeScore = new OutcomeScore();
                        outcomeScore.setOutcome(outcome);
                        outcomeScore.setParticipant(participant);

                        if (submission.getScore() == null) {
                            outcomeScore.setScoreNumeric(null);
                        } else {
                            outcomeScore.setScoreNumeric(submission.getScore().floatValue());
                        }

                        newScores.add(outcomeScore);
                        break;
                    }
                }
            }
        }

        newScores.forEach(
            outcomeScore -> outcomeScoreService.save(outcomeScore)
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
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long exposureId, Long outcomeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/exposures/{exposureId}/outcomes/{outcomeId}")
                .buildAndExpand(experimentId, exposureId, outcomeId).toUri());

        return headers;
    }

}
