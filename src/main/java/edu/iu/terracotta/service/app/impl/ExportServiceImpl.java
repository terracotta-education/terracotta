package edu.iu.terracotta.service.app.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.AnswerMcSubmissionOption;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionMc;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.enumerator.export.EventPersonalIdentifiers;
import edu.iu.terracotta.model.app.enumerator.export.ExperimentCsv;
import edu.iu.terracotta.model.app.enumerator.export.ItemResponsesCsv;
import edu.iu.terracotta.model.app.enumerator.export.ItemsCsv;
import edu.iu.terracotta.model.app.enumerator.export.OutcomesCsv;
import edu.iu.terracotta.model.app.enumerator.export.ParticipantTreatmentCsv;
import edu.iu.terracotta.model.app.enumerator.export.ParticipantsCsv;
import edu.iu.terracotta.model.app.enumerator.export.ResponseOptionsCsv;
import edu.iu.terracotta.model.app.enumerator.export.SubmissionsCsv;
import edu.iu.terracotta.model.events.Event;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExportService;
import edu.iu.terracotta.service.app.OutcomeService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.aws.AWSService;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExportServiceImpl implements ExportService {

    static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private OutcomeService outcomeService;

    @Autowired
    private AWSService awsService;

    @Autowired
    private Environment env;

    @Autowired
    private SubmissionService submissionService;

    @Override
    public Map<String, List<String[]>> getCsvFiles(long experimentId, SecuredInfo securedInfo) throws CanvasApiException, ParticipantNotUpdatedException, IOException {
        Map<String, List<String[]>> csvFiles = new HashMap<>();
        List<Participant> participants = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);

        // experiment.csv
        handleExperimentCsv(experimentId, participants, csvFiles);

        // outcomes.csv
        handleOutcomesCsv(experimentId, securedInfo, csvFiles);

        // participant_treatment.csv
        handleParticipantTreatmentCsv(participants, csvFiles);

        // participants.csv
        handleParticpantsCsv(participants, csvFiles);

        // submissions.csv
        handleSubmissionsCsv(experimentId, csvFiles);

        // items.csv
        handleItemsCsv(experimentId, csvFiles);

        // item_responses.csv
        handleItemResponsesCsv(experimentId, csvFiles);

        // response_options.csv
        handleResponseOptionsCsv(experimentId, csvFiles);

        return csvFiles;
    }

    private void handleExperimentCsv(long experimentId, List<Participant> participants, Map<String, List<String[]>> csvFiles) {
        List<String[]> experimentData = new ArrayList<>();
        experimentData.add(ExperimentCsv.getHeaderRow());
        Experiment experiment = allRepositories.experimentRepository.findByExperimentId(experimentId);
        String exportExperimentId = experiment.getExperimentId().toString();
        String courseId = String.valueOf(experiment.getLtiContextEntity().getContextId());
        String experimentTitle = "N/A";

        if (!StringUtils.isAllBlank(experiment.getTitle())) {
            experimentTitle = experiment.getTitle();
        }

        String experimentDescription = "N/A";

        if (!StringUtils.isAllBlank(experiment.getDescription())) {
            experimentDescription = experiment.getDescription();
        }

        String exposureType = experiment.getExposureType().toString();
        String participationType = experiment.getParticipationType().toString();
        String distributionType = experiment.getDistributionType().toString();
        String exportAt = Timestamp.valueOf(LocalDateTime.now()).toString();
        List<Participant> enrolled = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
        String enrollmentCount = String.valueOf(enrolled.size());

        List<Participant> consentedParticipants = CollectionUtils.emptyIfNull(participants).stream()
            .filter(participant -> participant.getConsent() != null && participant.getConsent())
            .collect(Collectors.toList());

        String participantCount = String.valueOf(consentedParticipants.size());
        List<Condition> conditions = allRepositories.conditionRepository.findByExperiment_ExperimentId(experimentId);
        String conditionCount = String.valueOf(conditions.size());

        experimentData.add(new String[] {exportExperimentId, courseId, experimentTitle, experimentDescription, exposureType,
            participationType, distributionType, exportAt, enrollmentCount, participantCount, conditionCount});
        csvFiles.put(ExperimentCsv.FILENAME, experimentData);
    }

    private void handleOutcomesCsv(long experimentId,  SecuredInfo securedInfo, Map<String, List<String[]>> csvFiles)
        throws CanvasApiException, IOException, ParticipantNotUpdatedException {
        List<Outcome> outcomes = outcomeService.findAllByExperiment(experimentId);

        for (Outcome outcome : outcomes) {
            outcomeService.updateOutcomeGrades(outcome.getOutcomeId(), securedInfo);
        }

        List<OutcomeScore> outcomeScores = allRepositories.outcomeScoreRepository.findByOutcome_Exposure_Experiment_ExperimentId(experimentId);
        List<String[]> outcomeData = new ArrayList<>();
        outcomeData.add(OutcomesCsv.getHeaderRow());

        CollectionUtils.emptyIfNull(outcomeScores).stream()
            .filter(outcomeScore -> outcomeScore.getParticipant().getConsent() != null && outcomeScore.getParticipant().getConsent())
            .forEach(outcomeScore -> {
                String outcomeId = outcomeScore.getOutcome().getOutcomeId().toString();
                String participantId = outcomeScore.getParticipant().getParticipantId().toString();
                Long exposureId = outcomeScore.getOutcome().getExposure().getExposureId();
                String source = outcomeScore.getOutcome().getLmsType().toString();
                String outcomeName = "N/A";

                if (!StringUtils.isAllBlank(outcomeScore.getOutcome().getTitle())) {
                    outcomeName = outcomeScore.getOutcome().getTitle();
                }
                String pointsPossible = outcomeScore.getOutcome().getMaxPoints().toString();
                String score = "N/A";

                if (outcomeScore.getScoreNumeric() != null) {
                    score = outcomeScore.getScoreNumeric().toString();
                }

                Long groupId = outcomeScore.getParticipant().getGroup().getGroupId();
                Optional<ExposureGroupCondition> groupConditionOptional =
                        allRepositories.exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(groupId, exposureId);

                if (groupConditionOptional.isPresent()) {
                    outcomeData.add(new String[]{outcomeId, participantId, String.valueOf(exposureId), source, outcomeName, pointsPossible, score,
                        groupConditionOptional.get().getCondition().getName(), String.valueOf(groupConditionOptional.get().getCondition().getConditionId())});
                    return;
                }

                outcomeData.add(new String[]{outcomeId, participantId, String.valueOf(exposureId), source, outcomeName, pointsPossible, score,
                    StringUtils.EMPTY, StringUtils.EMPTY});
            });

        csvFiles.put(OutcomesCsv.FILENAME, outcomeData);
    }

    private void handleParticipantTreatmentCsv(List<Participant> participants, Map<String, List<String[]>> csvFiles) {
        List<String[]> participantTreatments = new ArrayList<>();
        participantTreatments.add(ParticipantTreatmentCsv.getHeaderRow());

        CollectionUtils.emptyIfNull(participants).stream()
            .filter(participant -> participant.getConsent() != null && participant.getConsent() && participant.getGroup() != null)
            .forEach(participant -> {
                String participantId = participant.getParticipantId().toString();
                List<ExposureGroupCondition> egcList = allRepositories.exposureGroupConditionRepository.findByGroup_GroupId(participant.getGroup().getGroupId());

                CollectionUtils.emptyIfNull(egcList).stream()
                    .forEach(egc -> {
                        String exposureId = egc.getExposure().getExposureId().toString();
                        String conditionId = egc.getCondition().getConditionId().toString();
                        final String conditionName = StringUtils.isNotBlank(egc.getCondition().getName()) ? egc.getCondition().getName() : "N/A";

                        List<Assignment> assignments = allRepositories.assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(egc.getExposure().getExposureId(), false);

                        CollectionUtils.emptyIfNull(assignments).stream().forEach(assignment -> {
                            String assignmentId = assignment.getAssignmentId().toString();
                            String assignmentName = assignment.getTitle();
                            List<Treatment> treatments = allRepositories.treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentId(Long.parseLong(conditionId), Long.parseLong(assignmentId));

                            CollectionUtils.emptyIfNull(treatments).stream()
                                .forEach(treatment ->
                                    participantTreatments.add(
                                        new String[] {participantId, exposureId, conditionId, conditionName, assignmentId, assignmentName,
                                            treatment.getTreatmentId().toString(), treatment.getAssessment().getMultipleSubmissionScoringScheme().toString(),
                                            calculateAttemptsAllowed(treatment.getAssessment().getNumOfSubmissions()),
                                            calculateTimeRequiredBetweenAttempts(treatment.getAssignment().getHoursBetweenSubmissions()),
                                            calculateFinalScore(participant, treatment.getAssessment())
                                        })
                                );
                        });
                    });
            });

        csvFiles.put(ParticipantTreatmentCsv.FILENAME, participantTreatments);
    }

    private String calculateAttemptsAllowed(Integer numOfSubmissions) {
        if (numOfSubmissions == null || numOfSubmissions == 0) {
            return "unlimited";
        }

        return Integer.toString(numOfSubmissions);
    }

    private String calculateTimeRequiredBetweenAttempts(Float hoursBetweenSubmissions) {
        if (hoursBetweenSubmissions == null || hoursBetweenSubmissions == 0F) {
            return "N/A";
        }

        return String.format("%s hours", hoursBetweenSubmissions);
    }

    private String calculateFinalScore(Participant participant, Assessment assessment) {
        Float finalScore = submissionService.getScoreFromMultipleSubmissions(participant, assessment);

        if (finalScore == null) {
            return "N/A";
        }

        return Float.toString(finalScore);
    }

    private void handleParticpantsCsv(List<Participant> participants, Map<String, List<String[]>> csvFiles) {
        List<String[]> participantData = new ArrayList<>();
        participantData.add(ParticipantsCsv.getHeaderRow());

        CollectionUtils.emptyIfNull(participants).stream()
            .filter(participant -> participant.getConsent() != null && participant.getConsent())
            .forEach(participant -> {
                String participantId = participant.getParticipantId().toString();
                String consentedAt = participant.getDateGiven().toString();
                String consentSource = participant.getExperiment().getParticipationType().toString();
                participantData.add(new String[]{participantId, consentedAt, consentSource});
            });

        csvFiles.put(ParticipantsCsv.FILENAME, participantData);
    }

    private void handleSubmissionsCsv(long experimentId, Map<String, List<String[]>> csvFiles) {
        List<Submission> submissions = allRepositories.submissionRepository.findByParticipant_Experiment_ExperimentId(experimentId);
        List<String[]> submissionData = new ArrayList<>();
        submissionData.add(SubmissionsCsv.getHeaderRow());

        CollectionUtils.emptyIfNull(submissions).stream()
            .filter(submission -> submission.getParticipant().getConsent() != null && submission.getParticipant().getConsent() && submission.getDateSubmitted() != null)
            .forEach(submission -> {
                String submittedAt = submission.getDateSubmitted().toString();
                String participantId = submission.getParticipant().getParticipantId().toString();
                String assignmentId = submission.getAssessment().getTreatment().getAssignment().getAssignmentId().toString();
                String treatmentId = submission.getAssessment().getTreatment().getTreatmentId().toString();
                String calculatedScore = submission.getCalculatedGrade().toString();
                String overrideScore = submission.getAlteredCalculatedGrade().toString();
                String finalScore = submission.getTotalAlteredGrade().toString();
                String submissionId = submission.getSubmissionId().toString();
                submissionData.add(new String[]{submissionId, participantId, assignmentId, treatmentId, submittedAt, calculatedScore, overrideScore, finalScore});
            });

        csvFiles.put(SubmissionsCsv.FILENAME, submissionData);
    }

    private void handleItemsCsv(long experimentId, Map<String, List<String[]>> csvFiles) {
        List<Question> questions = allRepositories.questionRepository.findByAssessment_Treatment_Condition_Experiment_ExperimentId(experimentId);
        List<String[]> questionData = new ArrayList<>();
        questionData.add(ItemsCsv.getHeaderRow());

        CollectionUtils.emptyIfNull(questions).stream()
            .forEach(question -> {
                String itemId = question.getQuestionId().toString();
                String assignmentId = question.getAssessment().getTreatment().getAssignment().getAssignmentId().toString();
                String treatmentId = question.getAssessment().getTreatment().getTreatmentId().toString();
                String conditionId = question.getAssessment().getTreatment().getCondition().getConditionId().toString();
                String itemText = "N/A";

                if (StringUtils.isNotBlank(question.getHtml())) {
                    itemText = question.getHtml();
                }

                String itemFormat = question.getQuestionType().toString();
                questionData.add(new String[] {itemId, assignmentId, treatmentId, conditionId, itemText, itemFormat});
            });

        csvFiles.put(ItemsCsv.FILENAME, questionData);
    }

    private void handleItemResponsesCsv(long experimentId, Map<String, List<String[]>> csvFiles) {
        List<QuestionSubmission> questionSubmissions = allRepositories.questionSubmissionRepository.findBySubmission_Participant_Experiment_ExperimentId(experimentId);
        List<String[]> questionSubmissionData = new ArrayList<>();
        questionSubmissionData.add(ItemResponsesCsv.getHeaderRow());

        CollectionUtils.emptyIfNull(questionSubmissions).stream()
            .filter(questionSubmission -> questionSubmission.getSubmission().getParticipant().getConsent() != null && questionSubmission.getSubmission().getParticipant().getConsent())
            .forEach(questionSubmission -> {
                String itemResponseId = questionSubmission.getQuestionSubmissionId().toString();
                String submissionId = questionSubmission.getSubmission().getSubmissionId().toString();
                String assignmentId = questionSubmission.getQuestion().getAssessment().getTreatment().getAssignment().getAssignmentId().toString();
                String conditionId = questionSubmission.getQuestion().getAssessment().getTreatment().getCondition().getConditionId().toString();
                String treatmentId = questionSubmission.getQuestion().getAssessment().getTreatment().getTreatmentId().toString();
                String participantId = questionSubmission.getSubmission().getParticipant().getParticipantId().toString();
                String itemId = questionSubmission.getQuestion().getQuestionId().toString();
                String responseType = questionSubmission.getQuestion().getQuestionType().toString();
                String response = "N/A";
                String responseId = "N/A";
                String responsePosition = "N/A";
                String correctness = "N/A";
                String calculatedScore = "N/A";
                String overrideScore = "N/A";

                switch (questionSubmission.getQuestion().getQuestionType()) {
                    case MC:
                        List<AnswerMcSubmission> answerMcSubmissions = allRepositories.answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
                        if (CollectionUtils.isEmpty(answerMcSubmissions)) {
                            break;
                        }

                        AnswerMcSubmission answerMcSubmission = answerMcSubmissions.get(0);

                        if (StringUtils.isNotBlank(answerMcSubmission.getAnswerMc().getHtml())) {
                            response = answerMcSubmission.getAnswerMc().getHtml();
                        }

                        responseId = answerMcSubmission.getAnswerMc().getAnswerMcId().toString();
                        responsePosition = Character.toString(mapResponsePosition(Long.parseLong(itemId),
                                answerMcSubmission.getAnswerMc().getAnswerMcId(),
                                questionSubmission.getAnswerMcSubmissionOptions()));
                        correctness = answerMcSubmission.getAnswerMc().getCorrect().toString().toUpperCase();

                        if (questionSubmission.getCalculatedPoints() != null) {
                            calculatedScore = questionSubmission.getCalculatedPoints().toString();
                        }

                        if (questionSubmission.getAlteredGrade() != null) {
                            overrideScore = questionSubmission.getAlteredGrade().toString();
                        }

                        break;

                    case ESSAY:
                        List<AnswerEssaySubmission> answerEssaySubmissions = allRepositories.answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());

                        if (CollectionUtils.isEmpty(answerEssaySubmissions)) {
                            break;
                        }

                        AnswerEssaySubmission answerEssaySubmission = answerEssaySubmissions.get(0);

                        if (StringUtils.isNotBlank(answerEssaySubmission.getResponse())) {
                            response = answerEssaySubmission.getResponse();
                        }

                        if (questionSubmission.getCalculatedPoints() != null) {
                            calculatedScore = answerEssaySubmission.getQuestionSubmission().getCalculatedPoints().toString();
                        }

                        if (questionSubmission.getAlteredGrade() != null) {
                            overrideScore = answerEssaySubmission.getQuestionSubmission().getAlteredGrade().toString();
                        }

                        break;

                    default:
                        break;
                }

                String respondedAt = "N/A";

                if (questionSubmission.getSubmission().getDateSubmitted() != null) {
                    respondedAt = questionSubmission.getSubmission().getDateSubmitted().toString();
                }

                String pointsPossible = questionSubmission.getQuestion().getPoints().toString();
                questionSubmissionData.add(new String[]{itemResponseId, submissionId, assignmentId, conditionId, treatmentId, participantId, itemId, responseType, response, responseId, responsePosition, correctness,
                        respondedAt, pointsPossible, calculatedScore, overrideScore});
            });

        csvFiles.put(ItemResponsesCsv.FILENAME, questionSubmissionData);
    }

    private void handleResponseOptionsCsv(long experimentId, Map<String, List<String[]>> csvFiles) {
        List<AnswerMc> answerMcs = allRepositories.answerMcRepository.findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(experimentId);
        List<String[]> answerData = new ArrayList<>();
        answerData.add(ResponseOptionsCsv.getHeaderRow());

        CollectionUtils.emptyIfNull(answerMcs).stream()
            .forEach(answerMc -> {
                String responseId = answerMc.getAnswerMcId().toString();
                String itemId = answerMc.getQuestion().getQuestionId().toString();
                String response = "N/A";

                if (!StringUtils.isAllBlank(answerMc.getHtml())) {
                    response = answerMc.getHtml();
                }

                String responsePosition = Character.toString(mapResponsePosition(Long.parseLong(itemId), Long.parseLong(responseId)));
                String correct = answerMc.getCorrect().toString().toUpperCase();
                QuestionMc question = (QuestionMc) answerMc.getQuestion();
                String randomized = Boolean.toString(question.isRandomizeAnswers()).toUpperCase();
                answerData.add(new String[]{responseId, itemId, response, responsePosition, correct, randomized});
            });

        csvFiles.put(ResponseOptionsCsv.FILENAME, answerData);
    }

    @Override
    public Map<String, String> getJsonFiles(Long experimentId) {
        Map<String, String> jsonFiles = new HashMap<>();

        // events.json
        List<Event> events = allRepositories.eventRepository.findByParticipant_Experiment_ExperimentId(experimentId);
        List<String> caliperJsonEvents = new ArrayList<>();

        CollectionUtils.emptyIfNull(events).stream()
            .filter(event -> event.getParticipant().getConsent() != null && event.getParticipant().getConsent() && event.getJson() != null)
            .forEach(event -> {
                // Filter out personally identifying fields
                caliperJsonEvents.add(removePersonalIdentifiersFromEvent(event.getJson()));
            });

        String eventsJson = "[" + String.join(",", caliperJsonEvents) + "]";
        jsonFiles.put("events.json", eventsJson);

        return jsonFiles;
    }

    /**
     * Return the original order of the answer in the list of options.
     *
     * @param questionId
     * @param answerId
     * @return
     */
    public char mapResponsePosition(Long questionId, Long answerId) {
        return mapResponsePosition(questionId, answerId, Collections.emptyList());
    }

    /**
     * Consider possible random ordering of options in figuring out position.
     *
     * @param questionId
     * @param answerId
     * @param answerMcSubmissionOptions
     * @return
     */
    public char mapResponsePosition(Long questionId, Long answerId,
                                    List<AnswerMcSubmissionOption> answerMcSubmissionOptions) {
        List<AnswerMc> answerList = null;
        // Randomized option order is stored in AnswerMcSubmissionOptions, sort
        // AnswerMc's by its order
        if (answerMcSubmissionOptions.stream().anyMatch(o -> o.getAnswerMc().getAnswerMcId().equals(answerId))) {
            answerMcSubmissionOptions.sort(Comparator.comparingInt(AnswerMcSubmissionOption::getAnswerOrder));
            answerList = answerMcSubmissionOptions.stream()
                .map(AnswerMcSubmissionOption::getAnswerMc)
                .collect(Collectors.toList());
        } else {
            answerList = allRepositories.answerMcRepository.findByQuestion_QuestionId(questionId);
            answerList.sort(Comparator.comparingLong(AnswerMc::getAnswerOrder));
        }

        char position;

        for (AnswerMc answerMc : answerList) {
            if (answerMc.getAnswerMcId().equals(answerId)) {
                position = (char) ('A' + answerList.indexOf(answerMc));
                return position;
            }
        }

        return 'X';
    }

    private String removePersonalIdentifiersFromEvent(String eventJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(eventJson);

            for (String fieldName : EventPersonalIdentifiers.getFields()) {
                List<JsonNode> nodes = root.findParents(fieldName);

                if (nodes == null) {
                    log.debug("No nodes where found for field name {}", fieldName);
                    continue;
                }

                for (JsonNode jsonNode : nodes) {
                    ((ObjectNode) jsonNode).remove(fieldName);
                }
            }

            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            log.error("Failure while trying to remove personally identifying information from event JSON", e);
            // For safety, just return an empty object
            return "{}";
        }
    }


    @Override
    public Map<String, String> getReadMeFile() throws IOException {
        Map<String, String> map = new HashMap<>();
        String readmeBucketName = env.getProperty("aws.bucket-name");
        String readmeObjectKey = env.getProperty("aws.object-key");

        try (InputStream inputStream = awsService.readFileFromS3Bucket(readmeBucketName, readmeObjectKey)) {
            String text = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining(System.lineSeparator()));

            map.put(readmeObjectKey, text);
            return map;
        }
    }

}
