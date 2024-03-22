package edu.iu.terracotta.service.app.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVWriter;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.AnswerMcSubmissionOption;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
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
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
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
import edu.iu.terracotta.service.app.AssignmentTreatmentService;
import edu.iu.terracotta.service.app.ExportService;
import edu.iu.terracotta.service.app.OutcomeService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.aws.AWSService;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ExportServiceImpl implements ExportService {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String NA = "N/A";

    @Autowired private AllRepositories allRepositories;
    @Autowired private AssignmentTreatmentService assignmentTreatmentService;
    @Autowired private AWSService awsService;
    @Autowired private Environment env;
    @Autowired private OutcomeService outcomeService;
    @Autowired private SubmissionService submissionService;

    @Value("${app.export.batch.size:50}")
    private int exportBatchSize;

    @Value("${app.export.enable.events.output:true}")
    private boolean eventsOutputEnabled;

    @Value("${app.export.events.output.participant.threshold:400}")
    private int eventsOutputParticipantThreshold;

    @Value("${app.export.enable.readme:true}")
    private boolean readmeEnabled;

    private long consentedParticipantsCount;
    private List<Assignment> assignments;
    private List<ExposureGroupCondition> exposureGroupConditions;
    private List<Treatment> treatments;
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private SimpleDateFormat simpleDateFormatter = new SimpleDateFormat(DATE_FORMAT);

    @Override
    public Map<String, String> getFiles(long experimentId, SecuredInfo securedInfo)
            throws CanvasApiException, ParticipantNotUpdatedException, IOException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        /*
         * Prepare the datasets that will be utilized multiple times
         */
        prepareData(experimentId, securedInfo);

        /*
         * Mapping: filename => temporary_file_path (/tmp/data.csv)
         */
        Map<String, String> files = new HashMap<>();

        int page = 0;
        long participantCount = 0;
        List<Participant> participants = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId, PageRequest.of(page, exportBatchSize)).getContent();

        while (CollectionUtils.isNotEmpty(participants)) {
            // remove test students
            participants = participants.stream()
                .filter(participant -> !participant.isTestStudent())
                .toList();
            participantCount += participants.size();

            consentedParticipantsCount += CollectionUtils.emptyIfNull(participants).stream()
                .filter(participant -> BooleanUtils.isTrue(participant.getConsent()))
                .count();

            // participant_treatment.csv
            handleParticipantTreatmentCsv(participants, files);

            // participants.csv
            handleParticpantsCsv(participants, files);

            participants = allRepositories.participantRepository.findByExperiment_ExperimentId(experimentId, PageRequest.of(++page, exportBatchSize)).getContent();
        }

        // experiment.csv
        handleExperimentCsv(experimentId, participantCount, consentedParticipantsCount, files);

        // outcomes.csv
        handleOutcomesCsv(experimentId, securedInfo, files);

        // submissions.csv
        handleSubmissionsCsv(experimentId, files);

        // items.csv
        handleItemsCsv(experimentId, files);

        // item_responses.csv
        handleItemResponsesCsv(experimentId, files);

        // response_options.csv
        handleResponseOptionsCsv(experimentId, files);

        // events.json
        if (isEventExportAllowed()) {
            getJsonFiles(experimentId, files);
        }

        // README
        if (readmeEnabled) {
            getReadMeFile(files);
        }

        return files;
    }

    private void handleExperimentCsv(long experimentId, long participantCount, long consentedParticipantsCount, Map<String, String> files) throws IOException {
        Path path = createTempFile();
        files.put(ExperimentCsv.FILENAME, path.toString());

        try (CSVWriter writer = createCsvFileWriter(path)) {
            writer.writeNext(ExperimentCsv.getHeaderRow());

            Experiment experiment = allRepositories.experimentRepository.findByExperimentId(experimentId);

            writer.writeNext(new String[] {
                experiment.getExperimentId().toString(),
                String.valueOf(experiment.getLtiContextEntity().getContextId()),
                StringUtils.isBlank(experiment.getTitle()) ? NA : experiment.getTitle(),
                StringUtils.isBlank(experiment.getDescription()) ? NA : experiment.getDescription(),
                experiment.getExposureType().toString(),
                experiment.getParticipationType().toString(),
                experiment.getDistributionType().toString(),
                LocalDateTime.now().withNano(0).format(dateTimeFormatter),
                String.valueOf(participantCount),
                String.valueOf(consentedParticipantsCount),
                String.valueOf(allRepositories.conditionRepository.countByExperiment_ExperimentId(experimentId)),
                experiment.getCreatedAt().toLocalDateTime().format(dateTimeFormatter),
                experiment.isStarted() ? experiment.getStarted().toLocalDateTime().format(dateTimeFormatter) : NA
            });
        }
    }

    private void handleOutcomesCsv(long experimentId,  SecuredInfo securedInfo, Map<String, String> files)
            throws CanvasApiException, IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        int outcomesPage = 0;
        List<Outcome> outcomes = allRepositories.outcomeRepository.findByExposure_Experiment_ExperimentId(experimentId, PageRequest.of(outcomesPage, exportBatchSize)).getContent();

        while (CollectionUtils.isNotEmpty(outcomes)) {
            for (Outcome outcome : outcomes) {
                outcomeService.updateOutcomeGrades(outcome.getOutcomeId(), securedInfo);
            }

            outcomes = allRepositories.outcomeRepository.findByExposure_Experiment_ExperimentId(experimentId, PageRequest.of(++outcomesPage, exportBatchSize)).getContent();
        }

        Path path = createTempFile();
        files.put(OutcomesCsv.FILENAME, path.toString());

        try (CSVWriter writer = createCsvFileWriter(path)) {
            writer.writeNext(OutcomesCsv.getHeaderRow());

            int page = 0;
            List<OutcomeScore> outcomeScores = allRepositories.outcomeScoreRepository.findByOutcome_Exposure_Experiment_ExperimentId(experimentId, PageRequest.of(page, exportBatchSize)).getContent();

            while (CollectionUtils.isNotEmpty(outcomeScores)) {
                outcomeScores.stream()
                    .filter(outcomeScore -> !outcomeScore.getParticipant().isTestStudent())
                    .filter(outcomeScore -> BooleanUtils.isTrue(outcomeScore.getParticipant().getConsent()))
                    .forEach(outcomeScore -> {
                        if (outcomeScore.getParticipant().getGroup() != null) {
                            // participant has been assigned to a group; get exposure group condition
                            Optional<ExposureGroupCondition> exposureGroupCondition = findExposureGroupConditionByGroupIdAndExposureId(
                                outcomeScore.getParticipant().getGroup().getGroupId(),
                                outcomeScore.getOutcome().getExposure().getExposureId()
                            );

                            if (exposureGroupCondition.isPresent()) {
                                writer.writeNext(new String[] {
                                    outcomeScore.getOutcome().getOutcomeId().toString(),
                                    outcomeScore.getParticipant().getParticipantId().toString(),
                                    String.valueOf(outcomeScore.getOutcome().getExposure().getExposureId()),
                                    outcomeScore.getOutcome().getLmsType().toString(),
                                    StringUtils.isBlank(outcomeScore.getOutcome().getTitle()) ? NA : outcomeScore.getOutcome().getTitle(),
                                    outcomeScore.getOutcome().getMaxPoints().toString(),
                                    outcomeScore.getScoreNumeric() != null ? outcomeScore.getScoreNumeric().toString() : NA,
                                    exposureGroupCondition.get().getCondition().getName(),
                                    String.valueOf(exposureGroupCondition.get().getCondition().getConditionId())
                                });

                                return;
                            }
                        }

                        writer.writeNext(new String[] {
                            outcomeScore.getOutcome().getOutcomeId().toString(),
                            outcomeScore.getParticipant().getParticipantId().toString(),
                            String.valueOf(outcomeScore.getOutcome().getExposure().getExposureId()),
                            outcomeScore.getOutcome().getLmsType().toString(),
                            StringUtils.isBlank(outcomeScore.getOutcome().getTitle()) ? NA : outcomeScore.getOutcome().getTitle(),
                            outcomeScore.getOutcome().getMaxPoints().toString(),
                            outcomeScore.getScoreNumeric() != null ? outcomeScore.getScoreNumeric().toString() : NA,
                            StringUtils.EMPTY,
                            StringUtils.EMPTY
                        });
                    });

                outcomeScores = allRepositories.outcomeScoreRepository.findByOutcome_Exposure_Experiment_ExperimentId(experimentId, PageRequest.of(++page, exportBatchSize)).getContent();
            }
        }
    }

    private void handleParticipantTreatmentCsv(List<Participant> participants, Map<String, String> files) throws IOException {
        Path path;
        boolean writeHeader = false;

        if (files.get(ParticipantTreatmentCsv.FILENAME) != null) {
            path = Paths.get(files.get(ParticipantTreatmentCsv.FILENAME));
        } else {
            path = createTempFile();
            files.put(ParticipantTreatmentCsv.FILENAME, path.toString());
            writeHeader = true;
        }

        try (CSVWriter writer = createCsvFileWriter(path, true)) {
            if (writeHeader) {
                writer.writeNext(ParticipantTreatmentCsv.getHeaderRow());
            }

            CollectionUtils.emptyIfNull(participants).stream()
                .filter(participant -> BooleanUtils.isTrue(participant.getConsent()) && participant.getGroup() != null)
                .forEach(participant ->
                    CollectionUtils.emptyIfNull(findExposureGroupConditionByGroupId(participant.getGroup().getGroupId())).stream()
                        .forEach(egc ->
                            CollectionUtils.emptyIfNull(findAssignmentsByExposureIdAndActive(egc.getExposure().getExposureId())).stream()
                                .forEach(assignment ->
                                    CollectionUtils.emptyIfNull(findTreatmentByConditionIdAndAssignmentId(egc.getCondition().getConditionId(), assignment.getAssignmentId())).stream()
                                        .forEach(treatment ->
                                            writer.writeNext(
                                                new String[] {
                                                    participant.getParticipantId().toString(),
                                                    egc.getExposure().getExposureId().toString(),
                                                    egc.getCondition().getConditionId().toString(),
                                                    StringUtils.isNotBlank(egc.getCondition().getName()) ? egc.getCondition().getName() : NA,
                                                    assignment.getAssignmentId().toString(),
                                                    assignment.getTitle(),
                                                    assignment.getDueDate() != null ? simpleDateFormatter.format(assignment.getDueDate()) : NA,
                                                    treatment.getTreatmentId().toString(),
                                                    treatment.getAssessment().getMultipleSubmissionScoringScheme().toString(),
                                                    calculateAttemptsAllowed(treatment.getAssessment().getNumOfSubmissions()),
                                                    calculateTimeRequiredBetweenAttempts(treatment.getAssignment().getHoursBetweenSubmissions()),
                                                    calculateFinalScore(participant, treatment.getAssessment())
                                                })
                                        )
                                )
                        )
                );
        }
    }

    private String calculateAttemptsAllowed(Integer numOfSubmissions) {
        if (numOfSubmissions == null || numOfSubmissions == 0) {
            return "unlimited";
        }

        return Integer.toString(numOfSubmissions);
    }

    private String calculateTimeRequiredBetweenAttempts(Float hoursBetweenSubmissions) {
        if (hoursBetweenSubmissions == null || hoursBetweenSubmissions == 0F) {
            return NA;
        }

        return String.format("%s hours", hoursBetweenSubmissions);
    }

    private String calculateFinalScore(Participant participant, Assessment assessment) {
        Float finalScore = submissionService.getScoreFromMultipleSubmissions(participant, assessment);

        if (finalScore == null) {
            return NA;
        }

        return Float.toString(finalScore);
    }

    private void handleParticpantsCsv(List<Participant> participants, Map<String, String> files) throws IOException {
        Path path;
        boolean writeHeader = false;

        if (files.get(ParticipantsCsv.FILENAME) != null) {
            path = Paths.get(files.get(ParticipantsCsv.FILENAME));
        } else {
            path = createTempFile();
            files.put(ParticipantsCsv.FILENAME, path.toString());
            writeHeader = true;
        }

        try (CSVWriter writer = createCsvFileWriter(path, true)) {
            if (writeHeader) {
                writer.writeNext(ParticipantsCsv.getHeaderRow());
            }

            CollectionUtils.emptyIfNull(participants).stream()
                .filter(participant -> BooleanUtils.isTrue(participant.getConsent()))
                .forEach(participant ->
                    writer.writeNext(new String[] {
                        participant.getParticipantId().toString(),
                        participant.getDateGiven().toString(),
                        BooleanUtils.isTrue(participant.getConsent()) && participant.getGroup() == null ?
                            ParticipationTypes.CONSENTED_BUT_NOT_ASSIGNED.toString() : participant.getExperiment().getParticipationType().toString()
                    })
                );
        }
    }

    private void handleSubmissionsCsv(long experimentId, Map<String, String> files) throws IOException {
        Path path = createTempFile();
        files.put(SubmissionsCsv.FILENAME, path.toString());

        try (CSVWriter writer = createCsvFileWriter(path)) {
            writer.writeNext(SubmissionsCsv.getHeaderRow());

            int page = 0;
            List<Submission> submissions = allRepositories.submissionRepository.findByParticipant_Experiment_ExperimentId(experimentId, PageRequest.of(page, exportBatchSize)).getContent();

            while (CollectionUtils.isNotEmpty(submissions)) {
                CollectionUtils.emptyIfNull(submissions).stream()
                    .filter(submission -> submission.getDateSubmitted() != null)
                    .filter(submission -> submission.getParticipant().getLtiUserEntity() != null && !submission.getParticipant().isTestStudent())
                    .filter(submission -> BooleanUtils.isTrue(submission.getParticipant().getConsent()))
                    .forEach(submission ->
                        writer.writeNext(new String[] {
                            submission.getSubmissionId().toString(),
                            submission.getParticipant().getParticipantId().toString(),
                            submission.getAssessment().getTreatment().getAssignment().getAssignmentId().toString(),
                            submission.getAssessment().getTreatment().getTreatmentId().toString(),
                            submission.getDateSubmitted().toString(),
                            submission.getCalculatedGrade().toString(),
                            submission.getAlteredCalculatedGrade().toString(),
                            submission.getTotalAlteredGrade().toString()
                        })
                );

                submissions = allRepositories.submissionRepository.findByParticipant_Experiment_ExperimentId(experimentId, PageRequest.of(++page, exportBatchSize)).getContent();
            }
        }
    }

    private void handleItemsCsv(long experimentId, Map<String, String> files) throws IOException {
        Path path = createTempFile();
        files.put(ItemsCsv.FILENAME, path.toString());

        try (CSVWriter writer = createCsvFileWriter(path)) {
            writer.writeNext(ItemsCsv.getHeaderRow());

            int page = 0;
            List<Question> questions = allRepositories.questionRepository.findByAssessment_Treatment_Condition_Experiment_ExperimentId(experimentId, PageRequest.of(page, exportBatchSize)).getContent();

            while (CollectionUtils.isNotEmpty(questions)) {
                CollectionUtils.emptyIfNull(questions).stream()
                    .forEach(question ->
                        writer.writeNext(new String[] {
                            question.getQuestionId().toString(),
                            question.getAssessment().getTreatment().getAssignment().getAssignmentId().toString(),
                            question.getAssessment().getTreatment().getTreatmentId().toString(),
                            question.getAssessment().getTreatment().getCondition().getConditionId().toString(),
                            StringUtils.isNotBlank(question.getHtml()) ? question.getHtml() : NA,
                            question.getQuestionType().toString()
                        })
                    );

                questions = allRepositories.questionRepository.findByAssessment_Treatment_Condition_Experiment_ExperimentId(experimentId, PageRequest.of(++page, exportBatchSize)).getContent();
            }
        }
    }

    private void handleItemResponsesCsv(long experimentId, Map<String, String> files) throws IOException {
        Path path = createTempFile();
        files.put(ItemResponsesCsv.FILENAME, path.toString());

        try (CSVWriter writer = createCsvFileWriter(path)) {
            writer.writeNext(ItemResponsesCsv.getHeaderRow());

            int page = 0;
            List<QuestionSubmission> questionSubmissions = allRepositories.questionSubmissionRepository.findBySubmission_Participant_Experiment_ExperimentId(experimentId, PageRequest.of(page, exportBatchSize)).getContent();

            while (CollectionUtils.isNotEmpty(questionSubmissions)) {
                CollectionUtils.emptyIfNull(questionSubmissions).stream()
                    .filter(questionSubmission -> !questionSubmission.getSubmission().getParticipant().isTestStudent())
                    .filter(questionSubmission -> BooleanUtils.isNotFalse(questionSubmission.getSubmission().getParticipant().getConsent()))
                    .forEach(questionSubmission -> {
                        String response = NA;
                        String responseId = NA;
                        String responsePosition = NA;
                        String correctness = NA;
                        String calculatedScore = NA;
                        String overrideScore = NA;

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
                                responsePosition = Character.toString(mapResponsePosition(questionSubmission.getQuestion().getQuestionId(),
                                        answerMcSubmission.getAnswerMc().getAnswerMcId(),
                                        questionSubmission.getAnswerMcSubmissionOptions()));
                                correctness = answerMcSubmission.getAnswerMc().getCorrect().toString().toUpperCase(Locale.US);

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

                        writer.writeNext(new String[] {
                            questionSubmission.getQuestionSubmissionId().toString(),
                            questionSubmission.getSubmission().getSubmissionId().toString(),
                            questionSubmission.getQuestion().getAssessment().getTreatment().getAssignment().getAssignmentId().toString(),
                            questionSubmission.getQuestion().getAssessment().getTreatment().getCondition().getConditionId().toString(),
                            questionSubmission.getQuestion().getAssessment().getTreatment().getTreatmentId().toString(),
                            questionSubmission.getSubmission().getParticipant().getParticipantId().toString(),
                            questionSubmission.getQuestion().getQuestionId().toString(),
                            questionSubmission.getQuestion().getQuestionType().toString(),
                            response,
                            responseId,
                            responsePosition,
                            correctness,
                            questionSubmission.getSubmission().getDateSubmitted() != null ?  questionSubmission.getSubmission().getDateSubmitted().toString() : NA,
                            questionSubmission.getQuestion().getPoints().toString(),
                            calculatedScore,
                            overrideScore
                        });
                    });

                questionSubmissions = allRepositories.questionSubmissionRepository.findBySubmission_Participant_Experiment_ExperimentId(experimentId, PageRequest.of(++page, exportBatchSize)).getContent();
            }
        }
    }

    private void handleResponseOptionsCsv(long experimentId, Map<String, String> files) throws IOException {
        Path path = createTempFile();
        files.put(ResponseOptionsCsv.FILENAME, path.toString());

        try (CSVWriter writer = createCsvFileWriter(path)) {
            writer.writeNext(ResponseOptionsCsv.getHeaderRow());

            int page = 0;
            List<AnswerMc> answerMcs = allRepositories.answerMcRepository.findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(experimentId, PageRequest.of(page, exportBatchSize)).getContent();

            while (CollectionUtils.isNotEmpty(answerMcs)) {
                CollectionUtils.emptyIfNull(answerMcs).stream()
                    .forEach(answerMc ->
                        writer.writeNext(new String[] {
                            answerMc.getAnswerMcId().toString(),
                            answerMc.getQuestion().getQuestionId().toString(),
                            StringUtils.isNotBlank(answerMc.getHtml()) ? answerMc.getHtml() : NA,
                            Character.toString(mapResponsePosition(answerMc.getQuestion().getQuestionId(), answerMc.getAnswerMcId())),
                            answerMc.getCorrect().toString().toUpperCase(Locale.US),
                            Boolean.toString(((QuestionMc) answerMc.getQuestion()).isRandomizeAnswers()).toUpperCase(Locale.US)
                        })
                    );

                answerMcs = allRepositories.answerMcRepository.findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(experimentId, PageRequest.of(++page, exportBatchSize)).getContent();
            }
        }
    }

    public void getJsonFiles(Long experimentId, Map<String, String> files) throws IOException {
        Path path = createTempFile();
        files.put(EventPersonalIdentifiers.FILENAME, path.toString());

        try (PrintStream printStream = new PrintStream(path.toString())) {
            printStream.println("[");

            int page = 0;
            List<Event> events = allRepositories.eventRepository.findByParticipant_Experiment_ExperimentId(experimentId, PageRequest.of(page, exportBatchSize)).getContent();
            AtomicBoolean isFirstElement = new AtomicBoolean(true);

            while (CollectionUtils.isNotEmpty(events)) {
                CollectionUtils.emptyIfNull(events).stream()
                    .filter(event -> !event.getParticipant().isTestStudent())
                    .filter(event -> BooleanUtils.isNotFalse(event.getParticipant().getConsent()) && event.getJson() != null)
                    .forEach(event -> {
                        if (!isFirstElement.getAndSet(false)) {
                            printStream.println(",");
                        }
                        // Filter out personally identifying fields
                        printStream.print(removePersonalIdentifiersFromEvent(event.getJson()));
                    });

                events = allRepositories.eventRepository.findByParticipant_Experiment_ExperimentId(experimentId, PageRequest.of(++page, exportBatchSize)).getContent();
            }

            printStream.println();
            printStream.println("]");
        }
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
    public char mapResponsePosition(Long questionId, Long answerId, List<AnswerMcSubmissionOption> answerMcSubmissionOptions) {
        List<AnswerMc> answerList = null;
        // Randomized option order is stored in AnswerMcSubmissionOptions, sort AnswerMcs by its order
        if (answerMcSubmissionOptions.stream().anyMatch(o -> o.getAnswerMc().getAnswerMcId().equals(answerId))) {
            answerMcSubmissionOptions.sort(Comparator.comparingInt(AnswerMcSubmissionOption::getAnswerOrder));
            answerList = answerMcSubmissionOptions.stream()
                .map(AnswerMcSubmissionOption::getAnswerMc)
                .toList();
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
            // return an empty object
            return "{}";
        }
    }

    public void getReadMeFile(Map<String, String> files) throws IOException {
        String readmeBucketName = env.getProperty("aws.bucket-name");
        String readmeObjectKey = env.getProperty("aws.object-key");
        Path path = createTempFile();
        files.put(readmeObjectKey, path.toString());

        try (InputStream inputStream = awsService.readFileFromS3Bucket(readmeBucketName, readmeObjectKey)) {
            FileUtils.copyInputStreamToFile(inputStream, new File(path.toString()));
        }
    }

    private Path createTempFile() throws IOException {
        return Files.createTempFile("export." + UUID.randomUUID().toString(), null);
    }

    private CSVWriter createCsvFileWriter(Path path) throws IOException {
        return createCsvFileWriter(path, false);
    }

    private CSVWriter createCsvFileWriter(Path path, boolean append) throws IOException {
        return new CSVWriter(new FileWriter(new File(path.toString()), append));
    }

    private void prepareData(long experimentId, SecuredInfo securedInfo) {
        consentedParticipantsCount = 0l;
        exposureGroupConditions = allRepositories.exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(experimentId);
        assignments = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentId(experimentId);
        treatments = allRepositories.treatmentRepository.findByCondition_Experiment_ExperimentId(experimentId);

        if (CollectionUtils.isEmpty(assignments)) {
            return;
        }

        LtiUserEntity ltiUserEntity = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        assignments.stream()
            .forEach(
                assignment -> {
                    try {
                        assignmentTreatmentService.setAssignmentDtoAttrs(assignment, securedInfo.getCanvasCourseId(), ltiUserEntity);
                    } catch (NumberFormatException | CanvasApiException e) {
                        log.warn("Exception finding assignment ID: '{}' for course ID: '{}' in Canvas.", assignment.getLmsAssignmentId(), securedInfo.getCanvasCourseId(), e);
                    }
                }
            );
    }

    private List<ExposureGroupCondition> findExposureGroupConditionByGroupId(long groupId) {
        return exposureGroupConditions.stream()
            .filter(exposureGroupCondition -> exposureGroupCondition.getGroup().getGroupId().equals(groupId))
            .toList();
    }

    private Optional<ExposureGroupCondition> findExposureGroupConditionByGroupIdAndExposureId(long groupId, long exposureId) {
        return exposureGroupConditions.stream()
            .filter(exposureGroupCondition -> exposureGroupCondition.getGroup().getGroupId().equals(groupId))
            .filter(exposureGroupCondition -> exposureGroupCondition.getExposure().getExposureId().equals(exposureId))
            .findFirst();
    }

    private List<Assignment> findAssignmentsByExposureIdAndActive(long exposureId) {
        return assignments.stream()
            .filter(assignment -> BooleanUtils.isNotTrue(assignment.getSoftDeleted()))
            .filter(assignment -> assignment.getExposure().getExposureId().equals(exposureId))
            .toList();
    }

    private List<Treatment> findTreatmentByConditionIdAndAssignmentId(long conditionId, long assignmentId) {
        return treatments.stream()
            .filter(treatment -> treatment.getCondition().getConditionId().equals(conditionId))
            .filter(treatment -> treatment.getAssignment().getAssignmentId().equals(assignmentId))
            .toList();
    }

    private boolean isEventExportAllowed() {
        if (!eventsOutputEnabled) {
            return false;
        }

        return consentedParticipantsCount <= eventsOutputParticipantThreshold;
    }

}
