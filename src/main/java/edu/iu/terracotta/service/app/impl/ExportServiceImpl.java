package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.events.Event;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExportService;
import edu.iu.terracotta.service.app.OutcomeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportServiceImpl implements ExportService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    OutcomeService outcomeService;

    @Override
    public Map<String, List<String[]>> getCsvFiles(Long experimentId, SecuredInfo securedInfo) throws CanvasApiException, ParticipantNotUpdatedException, IOException {
        Map<String, List<String[]>> csvFiles = new HashMap<>();


        /*
        experiment.csv
         */
        List<String[]> experimentData = new ArrayList<>();
        experimentData.add(new String[] {"experiment_id", "course_id", "experiment_title", "experiment_description", "exposure_type", "participation_type", "distribution_type",
                                            "export_at", "enrollment_cnt", "participant_cnt", "condition_cnt"});
        Experiment experiment = allRepositories.experimentRepository.findByExperimentId(experimentId);
        String exportExperimentId = experiment.getExperimentId().toString();
        String courseId = String.valueOf(experiment.getLtiContextEntity().getContextId());
        String experimentTitle = "N/A";
        if(!StringUtils.isAllBlank(experiment.getTitle()))
            experimentTitle = experiment.getTitle();
        String experimentDescription = "N/A";
        if(!StringUtils.isAllBlank(experiment.getDescription()))
            experimentDescription = experiment.getDescription();
        String exposureType = experiment.getExposureType().toString();
        String participationType = experiment.getParticipationType().toString();
        String  distributionType = experiment.getDistributionType().toString();
        String exportAt = Timestamp.valueOf(LocalDateTime.now()).toString();
        List<Participant> enrolled = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
        String enrollmentCount = String.valueOf(enrolled.size());
        List<Participant> participants = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId);
        List<Participant> consentedParticipants = new ArrayList<>();
        for(Participant participant : participants){
            if(participant.getConsent() != null && participant.getConsent()){
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
        for(Outcome outcome : outcomes){
            outcomeService.updateOutcomeGrades(outcome.getOutcomeId(), securedInfo);
        }
        List<OutcomeScore> outcomeScores = allRepositories.outcomeScoreRepository.findByOutcome_Exposure_Experiment_ExperimentId(experimentId);
        List<String[]> outcomeData = new ArrayList<>();
        outcomeData.add(new String[]{"outcome_id", "participant_id", "exposure_id", "source", "outcome_name", "points_possible", "outcome_score"});
        for(OutcomeScore outcomeScore : outcomeScores){
            if(outcomeScore.getParticipant().getConsent() != null && outcomeScore.getParticipant().getConsent()){
                String outcomeId = outcomeScore.getOutcome().getOutcomeId().toString();
                String participantId = outcomeScore.getParticipant().getParticipantId().toString();
                String exposureId = outcomeScore.getOutcome().getExposure().getExposureId().toString();
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
                outcomeData.add(new String[]{outcomeId, participantId, exposureId, source, outcomeName, pointsPossible, score});
            }
        }
        csvFiles.put("outcomes.csv", outcomeData);


        /*
        participant_treatment.csv
         */
        List<String[]> participant_treatment = new ArrayList<>();
        participant_treatment.add(new String[] {"participant_id", "exposure_id", "condition_id", "condition_name", "assignment_id", "assignment_name", "treatment_id"});
        for(Participant participant : participants){
            if(participant.getConsent() != null && participant.getConsent()) {
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
                        List<Assignment> assignments = allRepositories.assignmentRepository.findByExposure_ExposureId(egc.getExposure().getExposureId());
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
        participantData.add(new String[] {"participant_id", "consented_at", "consent_source"});
        for(Participant participant : participants){
            if(participant.getConsent() != null && participant.getConsent()){
                String participantId = participant.getParticipantId().toString();
                String consentedAt = participant.getDateGiven().toString();
                String consentSource = participant.getExperiment().getParticipationType().toString();
                participantData.add(new String[] {participantId, consentedAt, consentSource});
            }
        }
        csvFiles.put("participants.csv", participantData);


        /*
        submissions.csv
         */
        List<Submission> submissions = allRepositories.submissionRepository.findByParticipant_Experiment_ExperimentId(experimentId);
        List<String[]> submissionData = new ArrayList<>();
        submissionData.add(new String[] {"submission_id", "participant_id", "assignment_id", "treatment_id", "submitted_at", "calculated_score", "override_score", "final_score"});
        for(Submission submission : submissions){
            if(submission.getParticipant().getConsent() != null && submission.getParticipant().getConsent()) {
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
        questionData.add(new String[] {"item_id", "assignment_id", "treatment_id", "condition_id", "item_text", "item_format"});
        for(Question question : questions){
            String itemId = question.getQuestionId().toString();
            String assignmentId = question.getAssessment().getTreatment().getAssignment().getAssignmentId().toString();
            String treatmentId = question.getAssessment().getTreatment().getTreatmentId().toString();
            String conditionId = question.getAssessment().getTreatment().getCondition().getConditionId().toString();
            String itemText = "N/A";
            if(!StringUtils.isAllBlank(question.getHtml())){
                itemText = question.getHtml();
            }
            String itemFormat = question.getQuestionType().toString();
            questionData.add(new String[] {itemId, assignmentId, treatmentId, conditionId, itemText, itemFormat});
        }
        csvFiles.put("items.csv", questionData);


        /*
        item_responses.csv
         */
        List<QuestionSubmission> questionSubmissions = allRepositories.questionSubmissionRepository.findBySubmission_Participant_Experiment_ExperimentId(experimentId);
        List<String[]> questionSubmissionData = new ArrayList<>();
        questionSubmissionData.add(new String[] {"item_response_id", "submission_id", "assignment_id", "condition_id", "treatment_id", "participant_id", "item_id", "response_type", "response", "response_id", "response_position",
                                                "correctness", "responded_at", "points_possible", "calculated_score", "override_score"});
        for(QuestionSubmission questionSubmission : questionSubmissions) {
            if(questionSubmission.getSubmission().getParticipant().getConsent() != null && questionSubmission.getSubmission().getParticipant().getConsent()) {
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
                        responsePosition = Character.toString(mapResponsePosition(Long.parseLong(itemId), answerMcSubmission.getAnswerMc().getAnswerMcId()));
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
        answerData.add(new String[] {"response_id", "item_id", "response", "response_position", "correct"});
        for(AnswerMc answerMc : answerMcs){
            String responseId = answerMc.getAnswerMcId().toString();
            String itemId = answerMc.getQuestion().getQuestionId().toString();
            String response = "N/A";
            if(!StringUtils.isAllBlank(answerMc.getHtml())){
                response = answerMc.getHtml();
            }
            String responsePosition = Character.toString(mapResponsePosition(Long.parseLong(itemId), Long.parseLong(responseId)));
            String correct = answerMc.getCorrect().toString().toUpperCase();
            answerData.add(new String[] {responseId, itemId, response, responsePosition, correct});
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
                caliperJsonEvents.add(event.getJson());
            }
        }
        String eventsJson = "[" + String.join(",", caliperJsonEvents) + "]";
        jsonFiles.put("events.json", eventsJson);

        return jsonFiles;
    }

    public char mapResponsePosition(Long questionId, Long answerId){
        List<AnswerMc> answerList = allRepositories.answerMcRepository.findByQuestion_QuestionId(questionId);
        answerList.sort(Comparator.comparingLong(AnswerMc::getAnswerOrder));
        char position;
        for(AnswerMc answerMc : answerList){
            if(answerMc.getAnswerMcId().equals(answerId)){
                position = (char)('A' + answerList.indexOf(answerMc));
                return position;
            }
        }
        return 'X';
    }
}
