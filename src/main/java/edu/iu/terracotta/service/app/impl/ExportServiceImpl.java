package edu.iu.terracotta.service.app.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.*;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.events.Event;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExportService;
import edu.iu.terracotta.service.app.OutcomeService;
import edu.iu.terracotta.service.aws.AWSService;
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
    AllRepositories allRepositories;

    @Autowired
    OutcomeService outcomeService;

    @Autowired
    AWSService awsService;

    @Autowired
    private Environment env;


    @Override
    public Map<String, List<String[]>> getCsvFiles(Long experimentId, SecuredInfo securedInfo) throws CanvasApiException, ParticipantNotUpdatedException, IOException {
        Map<String, List<String[]>> csvFiles = new HashMap<>();


        /*
        experiment.csv
         */
        List<String[]> experimentData = new ArrayList<>();
        experimentData.add(new String[]{"experiment_id", "course_id", "experiment_title", "experiment_description", "exposure_type", "participation_type", "distribution_type",
                "export_at", "enrollment_cnt", "participant_cnt", "condition_cnt"});
        Experiment experiment = allRepositories.experimentRepository.findByExperimentId(experimentId);
        String exportExperimentId = experiment.getExperimentId().toString();
        String courseId = String.valueOf(experiment.getLtiContextEntity().getContextId());
        String experimentTitle = "N/A";
        if (!StringUtils.isAllBlank(experiment.getTitle()))
            experimentTitle = experiment.getTitle();
        String experimentDescription = "N/A";
        if (!StringUtils.isAllBlank(experiment.getDescription()))
            experimentDescription = experiment.getDescription();
        String exposureType = experiment.getExposureType().toString();
        String participationType = experiment.getParticipationType().toString();
        String distributionType = experiment.getDistributionType().toString();
        String exportAt = Timestamp.valueOf(LocalDateTime.now()).toString();
        List<Participant> enrolled = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
        String enrollmentCount = String.valueOf(enrolled.size());
        List<Participant> participants = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
        List<Participant> consentedParticipants = new ArrayList<>();
        for (Participant participant : participants) {
            if (participant.getConsent() != null && participant.getConsent()) {
                consentedParticipants.add(participant);
            }
        }
        String participantCount = String.valueOf(consentedParticipants.size());
        List<Condition> conditions = allRepositories.conditionRepository.findByExperiment_ExperimentId(experimentId);
        String conditionCount = String.valueOf(conditions.size());

        experimentData.add(new String[]{exportExperimentId, courseId, experimentTitle, experimentDescription, exposureType, participationType, distributionType, exportAt, enrollmentCount,
                participantCount, conditionCount});
        csvFiles.put("experiment.csv", experimentData);



        /*
        outcomes.csv
         */
        List<Outcome> outcomes = outcomeService.findAllByExperiment(experimentId);
        for (Outcome outcome : outcomes) {
            outcomeService.updateOutcomeGrades(outcome.getOutcomeId(), securedInfo);
        }
        List<OutcomeScore> outcomeScores = allRepositories.outcomeScoreRepository.findByOutcome_Exposure_Experiment_ExperimentId(experimentId);
        List<String[]> outcomeData = new ArrayList<>();
        outcomeData.add(new String[]{"outcome_id", "participant_id", "exposure_id", "source", "outcome_name", "points_possible", "outcome_score"});
        for (OutcomeScore outcomeScore : outcomeScores) {
            if (outcomeScore.getParticipant().getConsent() != null && outcomeScore.getParticipant().getConsent()) {
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
                outcomeData.add(new String[]{outcomeId, participantId, String.valueOf(exposureId), source, outcomeName, pointsPossible, score});
                Long groupId = outcomeScore.getParticipant().getGroup().getGroupId();
                Optional<ExposureGroupCondition> groupConditionOptional =
                        allRepositories.exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(groupId, exposureId);
                if (groupConditionOptional.isPresent()) {
                    ExposureGroupCondition groupCondition = groupConditionOptional.get();
                    outcomeData.add(new String[]{outcomeId, participantId, String.valueOf(exposureId), source, outcomeName, pointsPossible, score,
                            groupCondition.getCondition().getName(), String.valueOf(groupCondition.getCondition().getConditionId())});
                }
            }
        }
        csvFiles.put("outcomes.csv", outcomeData);


        /*
        participant_treatment.csv
         */
        List<String[]> participant_treatment = new ArrayList<>();
        participant_treatment.add(new String[]{"participant_id", "exposure_id", "condition_id", "condition_name", "assignment_id", "assignment_name", "treatment_id"});
        for (Participant participant : participants) {
            if (participant.getConsent() != null && participant.getConsent()) {
                if (participant.getGroup() != null) {
                    String participantId = participant.getParticipantId().toString();
                    List<ExposureGroupCondition> egcList = allRepositories.exposureGroupConditionRepository.findByGroup_GroupId(participant.getGroup().getGroupId());
                    for (ExposureGroupCondition egc : egcList) {
                        String exposureId = egc.getExposure().getExposureId().toString();
                        String conditionId = egc.getCondition().getConditionId().toString();
                        String conditionName = "N/A";
                        if (!StringUtils.isAllBlank(egc.getCondition().getName())) {
                            conditionName = egc.getCondition().getName();
                        }
                        List<Assignment> assignments = allRepositories.assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(egc.getExposure().getExposureId(), false);
                        for (Assignment assignment : assignments) {
                            String assignmentId = assignment.getAssignmentId().toString();
                            String assignmentName = assignment.getTitle();
                            List<Treatment> treatments = allRepositories.treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentId(Long.parseLong(conditionId), Long.parseLong(assignmentId));
                            for (Treatment treatment : treatments) {
                                String treatmentId = treatment.getTreatmentId().toString();
                                participant_treatment.add(new String[]{participantId, exposureId, conditionId, conditionName, assignmentId, assignmentName, treatmentId});
                            }
                        }
                    }
                }
            }
        }
        csvFiles.put("participant_treatment.csv", participant_treatment);


        /*
        participants.csv
         */
        List<String[]> participantData = new ArrayList<>();
        participantData.add(new String[]{"participant_id", "consented_at", "consent_source"});
        for (Participant participant : participants) {
            if (participant.getConsent() != null && participant.getConsent()) {
                String participantId = participant.getParticipantId().toString();
                String consentedAt = participant.getDateGiven().toString();
                String consentSource = participant.getExperiment().getParticipationType().toString();
                participantData.add(new String[]{participantId, consentedAt, consentSource});
            }
        }
        csvFiles.put("participants.csv", participantData);


        /*
        submissions.csv
         */
        List<Submission> submissions = allRepositories.submissionRepository.findByParticipant_Experiment_ExperimentId(experimentId);
        List<String[]> submissionData = new ArrayList<>();
        submissionData.add(new String[]{"submission_id", "participant_id", "assignment_id", "treatment_id", "submitted_at", "calculated_score", "override_score", "final_score"});
        for (Submission submission : submissions) {
            if (submission.getParticipant().getConsent() != null && submission.getParticipant().getConsent()) {
                if (submission.getDateSubmitted() != null) {
                    String submittedAt = submission.getDateSubmitted().toString();
                    String participantId = submission.getParticipant().getParticipantId().toString();
                    String assignmentId = submission.getAssessment().getTreatment().getAssignment().getAssignmentId().toString();
                    String treatmentId = submission.getAssessment().getTreatment().getTreatmentId().toString();
                    String calculatedScore = submission.getCalculatedGrade().toString();
                    String overrideScore = submission.getAlteredCalculatedGrade().toString();
                    String finalScore = submission.getTotalAlteredGrade().toString();
                    String submissionId = submission.getSubmissionId().toString();
                    submissionData.add(new String[]{submissionId, participantId, assignmentId, treatmentId, submittedAt, calculatedScore, overrideScore, finalScore});
                }
            }
        }
        csvFiles.put("submissions.csv", submissionData);


        /*
        items.csv
         */
        List<Question> questions = allRepositories.questionRepository.findByAssessment_Treatment_Condition_Experiment_ExperimentId(experimentId);
        List<String[]> questionData = new ArrayList<>();
        questionData.add(new String[]{"item_id", "assignment_id", "treatment_id", "condition_id", "item_text", "item_format"});
        for (Question question : questions) {
            String itemId = question.getQuestionId().toString();
            String assignmentId = question.getAssessment().getTreatment().getAssignment().getAssignmentId().toString();
            String treatmentId = question.getAssessment().getTreatment().getTreatmentId().toString();
            String conditionId = question.getAssessment().getTreatment().getCondition().getConditionId().toString();
            String itemText = "N/A";
            if (!StringUtils.isAllBlank(question.getHtml())) {
                itemText = question.getHtml();
            }
            String itemFormat = question.getQuestionType().toString();
            questionData.add(new String[]{itemId, assignmentId, treatmentId, conditionId, itemText, itemFormat});
        }
        csvFiles.put("items.csv", questionData);


        /*
        item_responses.csv
         */
        List<QuestionSubmission> questionSubmissions = allRepositories.questionSubmissionRepository.findBySubmission_Participant_Experiment_ExperimentId(experimentId);
        List<String[]> questionSubmissionData = new ArrayList<>();
        questionSubmissionData.add(new String[]{"item_response_id", "submission_id", "assignment_id", "condition_id", "treatment_id", "participant_id", "item_id", "response_type", "response", "response_id", "response_position",
                "correctness", "responded_at", "points_possible", "calculated_score", "override_score"});
        for (QuestionSubmission questionSubmission : questionSubmissions) {
            if (questionSubmission.getSubmission().getParticipant().getConsent() != null && questionSubmission.getSubmission().getParticipant().getConsent()) {
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
                if (questionSubmission.getQuestion().getQuestionType().equals(QuestionTypes.MC)) {
                    List<AnswerMcSubmission> answerMcSubmissions = allRepositories.answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
                    if (!answerMcSubmissions.isEmpty()) {
                        AnswerMcSubmission answerMcSubmission = answerMcSubmissions.get(0);
                        if (!StringUtils.isAllBlank(answerMcSubmission.getAnswerMc().getHtml())) {
                            response = answerMcSubmission.getAnswerMc().getHtml();
                        }
                        responseId = answerMcSubmission.getAnswerMc().getAnswerMcId().toString();
                        responsePosition = Character.toString(mapResponsePosition(Long.parseLong(itemId),
                                answerMcSubmission.getAnswerMc().getAnswerMcId(),
                                questionSubmission.getAnswerMcSubmissionOptions()));
                        correctness = answerMcSubmission.getAnswerMc().getCorrect().toString().toUpperCase();
                    }
                    if (questionSubmission.getCalculatedPoints() != null) {
                        calculatedScore = questionSubmission.getCalculatedPoints().toString();
                    }
                    if (questionSubmission.getAlteredGrade() != null) {
                        overrideScore = questionSubmission.getAlteredGrade().toString();
                    }
                } else if (questionSubmission.getQuestion().getQuestionType().equals(QuestionTypes.ESSAY)) {
                    List<AnswerEssaySubmission> answerEssaySubmissions = allRepositories.answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());
                    if (!answerEssaySubmissions.isEmpty()) {
                        AnswerEssaySubmission answerEssaySubmission = answerEssaySubmissions.get(0);
                        if (!StringUtils.isAllBlank(answerEssaySubmission.getResponse())) {
                            response = answerEssaySubmission.getResponse();
                        }
                        if (questionSubmission.getCalculatedPoints() != null)
                            calculatedScore = answerEssaySubmission.getQuestionSubmission().getCalculatedPoints().toString();
                        if (questionSubmission.getAlteredGrade() != null)
                            overrideScore = answerEssaySubmission.getQuestionSubmission().getAlteredGrade().toString();
                    }
                }

                String respondedAt = "N/A";
                if (questionSubmission.getSubmission().getDateSubmitted() != null) {
                    respondedAt = questionSubmission.getSubmission().getDateSubmitted().toString();
                }
                String pointsPossible = questionSubmission.getQuestion().getPoints().toString();
                questionSubmissionData.add(new String[]{itemResponseId, submissionId, assignmentId, conditionId, treatmentId, participantId, itemId, responseType, response, responseId, responsePosition, correctness,
                        respondedAt, pointsPossible, calculatedScore, overrideScore});
            }
        }
        csvFiles.put("item_responses.csv", questionSubmissionData);


        List<AnswerMc> answerMcs = allRepositories.answerMcRepository.findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(experimentId);
        List<String[]> answerData = new ArrayList<>();
        answerData.add(
                new String[]{"response_id", "item_id", "response", "response_position", "correct", "randomized"});
        for (AnswerMc answerMc : answerMcs) {
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
        }
        csvFiles.put("response_options.csv", answerData);

        return csvFiles;
    }

    @Override
    public Map<String, String> getJsonFiles(Long experimentId) {
        Map<String, String> jsonFiles = new HashMap<>();

        /*
         * events.json
         */
        List<Event> events = allRepositories.eventRepository.findByParticipant_Experiment_ExperimentId(experimentId);
        List<String> caliperJsonEvents = new ArrayList<>();
        for (Event event : events) {
            if (event.getParticipant().getConsent() != null && event.getParticipant().getConsent()) {
                // backwards compatibility: 'json' column was introduced later
                if (event.getJson() == null) {
                    continue;
                }
                // Filter out personally identifying fields
                caliperJsonEvents.add(removePersonalIdentifiersFromEvent(event.getJson()));
            }
        }
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
        if (answerMcSubmissionOptions.stream().anyMatch(o -> o.getAnswerMc().getAnswerMcId() == answerId)) {
            answerMcSubmissionOptions.sort(Comparator.comparingInt(AnswerMcSubmissionOption::getAnswerOrder));
            answerList = answerMcSubmissionOptions.stream().map(o -> o.getAnswerMc()).collect(Collectors.toList());
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

        String[] personalIdentifierFieldNames = {
                "canvas_login_id", "canvas_user_name", "canvas_global_id", "canvas_user_id", "canvas_user_global_id"
        };
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(eventJson);
            for (String fieldName : personalIdentifierFieldNames) {
                List<JsonNode> nodes = root.findParents(fieldName);
                if (nodes != null) {
                    for (JsonNode jsonNode : nodes) {
                        ((ObjectNode) jsonNode).remove(fieldName);
                    }
                } else {
                    log.debug("No nodes where found for field name {}", fieldName);
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
        InputStream inputStream = null;
        Map<String, String> map = new HashMap<>();
        try {
            String readmeBucketName = env.getProperty("aws.bucket-name");
            String readmeObjectKey = env.getProperty("aws.object-key");
            inputStream = awsService.readFileFromS3Bucket(readmeBucketName, readmeObjectKey);
            String text = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining(System.lineSeparator()));

            map.put(readmeObjectKey, text);
            return map;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

}