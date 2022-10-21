package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.*;
import edu.iu.terracotta.model.app.*;
import edu.iu.terracotta.model.app.dto.OutcomeDto;
import edu.iu.terracotta.model.app.dto.OutcomePotentialDto;
import edu.iu.terracotta.model.app.dto.OutcomeScoreDto;
import edu.iu.terracotta.model.app.enumerator.LmsType;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.*;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.utils.TextConstants;
import edu.ksu.canvas.model.assignment.Submission;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OutcomeServiceImpl implements OutcomeService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    OutcomeScoreService outcomeScoreService;

    @Autowired
    ParticipantService participantService;

    @Autowired
    ExposureService exposureService;

    @Autowired
    ExperimentService experimentService;

    @Autowired
    CanvasAPIClient canvasAPIClient;

    @Value("${application.url}")
    private String localUrl;

    @Override
    public List<Outcome> findAllByExposureId(Long exposureId) {
        return allRepositories.outcomeRepository.findByExposure_ExposureId(exposureId);
    }

    @Override
    public List<Outcome> findAllByExperiment(long experimentId) {
        return allRepositories.outcomeRepository.findByExposure_Experiment_ExperimentId(experimentId);
    }

    @Override
    public List<OutcomeDto> getOutcomes(Long exposureId) {
        List<Outcome> outcomes = findAllByExposureId(exposureId);
        List<OutcomeDto> outcomeDtoList = new ArrayList<>();
        for (Outcome outcome : outcomes) {
            outcomeDtoList.add(toDto(outcome, false));
        }
        return outcomeDtoList;
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
            throw new DataServiceException("Error 105: Unable to create Outcome: " + ex.getMessage());
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
        if (exposure.isPresent()) {
            outcome.setExposure(exposure.get());
        } else {
            throw new DataServiceException("Exposure for outcome does not exist.");
        }

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
        if (StringUtils.isAllBlank(outcomeDto.getTitle()) && StringUtils.isAllBlank(outcome.getTitle())) {
            throw new TitleValidationException("Error 100: Please give the outcome a title.");
        }
        if (!StringUtils.isAllBlank(outcomeDto.getTitle()) && outcomeDto.getTitle().length() > 255) {
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
    public List<OutcomePotentialDto> potentialOutcomes(Long experimentId) throws DataServiceException, CanvasApiException {
        Optional<Experiment> experiment = experimentService.findById(experimentId);
        List<Assignment> assignmentList = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentId(experimentId);
        List<OutcomePotentialDto> outcomePotentialDtos = new ArrayList<>();
        if (experiment.isPresent()) {
            String canvasCourseId = StringUtils.substringBetween(experiment.get().getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");
            List<AssignmentExtended> assignmentExtendedList = canvasAPIClient.listAssignments(canvasCourseId, experiment.get().getPlatformDeployment());
            for (AssignmentExtended assignmentExtended : assignmentExtendedList) {
                List<Assignment> matched = assignmentList.stream().filter(x -> {
                    if (assignmentExtended.getId().intValue() == Integer.valueOf(x.getLmsAssignmentId()).intValue()) {
                        return true;
                    }
                    return false;
                }).collect(Collectors.toList());
                if (matched.isEmpty() && !(experiment.get().getConsentDocument() != null && assignmentExtended.getId().intValue() ==
                        Integer.valueOf(experiment.get().getConsentDocument().getLmsAssignmentId()).intValue())) {
                    outcomePotentialDtos.add(assignmentExtendedToOutcomePotentialDto(assignmentExtended));
                }
            }
        } else {
            throw new DataServiceException("Error 105: Experiment does not exist.");
        }
        return outcomePotentialDtos;
    }

    private OutcomePotentialDto assignmentExtendedToOutcomePotentialDto(AssignmentExtended assignmentExtended) {
        OutcomePotentialDto potentialDto = new OutcomePotentialDto();
        potentialDto.setAssignmentId(assignmentExtended.getId());
        potentialDto.setName(assignmentExtended.getName());
        potentialDto.setType(assignmentExtended.getSubmissionTypes().get(0));
        potentialDto.setPointsPossible(assignmentExtended.getPointsPossible());
        potentialDto.setTerracotta(assignmentExtended.getSubmissionTypes().contains("external_tool") && assignmentExtended.getExternalToolTagAttributes().getUrl().contains(localUrl));
        return potentialDto;
    }

    @Override
    @Transactional
    public void updateOutcomeGrades(Long outcomeId, SecuredInfo securedInfo) throws CanvasApiException, IOException, ParticipantNotUpdatedException {
        Optional<Outcome> outcomeSearchResult = this.findById(outcomeId);
        Outcome outcome = outcomeSearchResult.get();
        //If this is not external we don't need to check the scores.
        if (outcome.getExternal() == null || !outcome.getExternal()) {
            return;
        }
        participantService.refreshParticipants(outcome.getExposure().getExperiment().getExperimentId(), securedInfo, outcome.getExposure().getExperiment().getParticipants());
        List<OutcomeScore> newScores = new ArrayList<>();
        String canvasCourseId = StringUtils.substringBetween(outcome.getExposure().getExperiment().getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");
        List<Submission> submissions = canvasAPIClient.listSubmissions(Integer.parseInt(outcome.getLmsOutcomeId()), canvasCourseId, outcome.getExposure().getExperiment().getPlatformDeployment());
        for (Submission submission : submissions) {
            boolean found = false;
            for (OutcomeScore outcomeScore : outcome.getOutcomeScores()) {
                if (outcomeScore.getParticipant().getLtiUserEntity().getEmail() != null && outcomeScore.getParticipant().getLtiUserEntity().getEmail().equals(submission.getUser().getLoginId()) && outcomeScore.getParticipant().getLtiUserEntity().getDisplayname().equals(submission.getUser().getName())) {
                    found = true;
                    if (submission.getScore() != null) {
                        outcomeScore.setScoreNumeric(submission.getScore().floatValue());
                    } else {
                        outcomeScore.setScoreNumeric(null);
                    }
                    break;
                }
            }
            if (!found) {
                for (OutcomeScore outcomeScore : outcome.getOutcomeScores()) {
                    if (outcomeScore.getParticipant().getLtiUserEntity().getDisplayname().equals(submission.getUser().getName())) {
                        found = true;
                        if (submission.getScore() != null) {
                            outcomeScore.setScoreNumeric(submission.getScore().floatValue());
                        } else {
                            outcomeScore.setScoreNumeric(null);
                        }
                        break;
                    }

                }
            }
            if (!found) {
                for (Participant participant : outcome.getExposure().getExperiment().getParticipants()) {
                    if (participant.getLtiUserEntity().getEmail() != null && participant.getLtiUserEntity().getEmail().equals(submission.getUser().getLoginId()) && participant.getLtiUserEntity().getDisplayname().equals(submission.getUser().getName())) {
                        found = true;
                        OutcomeScore outcomeScore = new OutcomeScore();
                        outcomeScore.setOutcome(outcome);
                        outcomeScore.setParticipant(participant);
                        if (submission.getScore() != null) {
                            outcomeScore.setScoreNumeric(submission.getScore().floatValue());
                        } else {
                            outcomeScore.setScoreNumeric(null);
                        }
                        newScores.add(outcomeScore);
                        break;
                    }
                }
            }
            if (!found) {
                for (Participant participant : outcome.getExposure().getExperiment().getParticipants()) {
                    if (participant.getLtiUserEntity().getDisplayname().equals(submission.getUser().getName())) {
                        OutcomeScore outcomeScore = new OutcomeScore();
                        outcomeScore.setOutcome(outcome);
                        outcomeScore.setParticipant(participant);
                        if (submission.getScore() != null) {
                            outcomeScore.setScoreNumeric(submission.getScore().floatValue());
                        } else {
                            outcomeScore.setScoreNumeric(null);
                        }
                        newScores.add(outcomeScore);
                        break;
                    }
                }
            }

        }

        for (OutcomeScore outcomeScore : newScores) {
            outcomeScoreService.save(outcomeScore);
        }
        //TODO, what to do if the outcome score is there but the participant is dropped.

    }

    @Override
    public void defaultOutcome(OutcomeDto outcomeDto) throws TitleValidationException {
        if (!StringUtils.isAllBlank(outcomeDto.getTitle()) && outcomeDto.getTitle().length() > 255) {
            throw new TitleValidationException("Error 101: The title must be 255 characters or less.");
        }
        if (outcomeDto.getExternal() != null) {
            if (!outcomeDto.getExternal()) {
                outcomeDto.setLmsOutcomeId(null);
                outcomeDto.setLmsType("NONE");
            }
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long exposureId, Long outcomeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}")
                .buildAndExpand(experimentId, exposureId, outcomeId).toUri());
        return headers;
    }

}