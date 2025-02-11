package edu.iu.terracotta.service.app.integrations.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.entity.integrations.IntegrationClient;
import edu.iu.terracotta.dao.entity.integrations.IntegrationToken;
import edu.iu.terracotta.dao.entity.integrations.IntegrationTokenLog;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.enums.integrations.IntegrationTokenStatus;
import edu.iu.terracotta.dao.repository.QuestionRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationClientRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationTokenLogRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.integrations.IntegrationScoreAsyncService;
import edu.iu.terracotta.service.app.integrations.IntegrationScoreService;
import edu.iu.terracotta.service.app.integrations.IntegrationTokenService;
import edu.iu.terracotta.service.caliper.CaliperService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class IntegrationScoreServiceImpl implements IntegrationScoreService {

    @Autowired private IntegrationClientRepository integrationClientRepository;
    @Autowired private IntegrationTokenLogRepository integrationTokenLogRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionSubmissionRepository questionSubmissionRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private CaliperService caliperService;
    @Autowired private IntegrationScoreAsyncService integrationScoreAsyncService;
    @Autowired private IntegrationTokenService integrationTokenService;

    @Override
    public void score(String launchToken, String score, Optional<String> previewTokenClient) throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        try {
            log.info("Processing external integration score: [{}] for launch token: [{}]", score, launchToken);

            if (StringUtils.isBlank(launchToken)) {
                throw new IntegrationTokenInvalidException("Launch token cannot be null");
            }

            // invalidate the token; skip if preview
            IntegrationToken integrationToken = previewTokenClient.isEmpty() ? integrationTokenService.redeemToken(launchToken) : null;
            Float calculatedScore;

            try {
                if (StringUtils.isBlank(score)) {
                    // no score was returned; default to question points
                    if (previewTokenClient.isPresent()) {
                        // this is a preview submission; set calculated score to -1 for further processing
                        log.info(
                            "No score returned for preview token: [{}]. Setting calculated score to null.",
                            launchToken
                        );
                        calculatedScore = null;
                    } else {
                        log.info(
                            "No score returned for token: [{}]. Setting calculated score to question points: [{}]",
                            launchToken,
                            integrationToken.getIntegration().getQuestion().getPoints()
                        );
                        calculatedScore = integrationToken.getIntegration().getQuestion().getPoints();
                    }
                } else {
                    calculatedScore = Float.parseFloat(score);
                }
            } catch (Exception e) {
                throw new RuntimeException(
                    String.format(
                        "Error converting calculated score: [%s]. Cannot set score for submission ID: [%s]",
                        score,
                        integrationToken != null && integrationToken.getSubmission() != null ? integrationToken.getSubmission().getSubmissionId() : "N/A"
                    ),
                    e
                );
            }

            if (previewTokenClient.isPresent()) {
                return;
            }

            if (CollectionUtils.isNotEmpty(integrationToken.getSubmission().getQuestionSubmissions())) {
                // question submissions exist; update the calculated score
                integrationToken.getSubmission().getQuestionSubmissions()
                    .forEach(
                        questionSubmission -> {
                            questionSubmission.setCalculatedPoints(calculatedScore);
                            questionSubmission.setAlteredGrade(0F);
                            questionSubmissionRepository.save(questionSubmission);
                        }
                    );
            } else {
                // create a question submission and set calculated score for the submission
                QuestionSubmission questionSubmission = new QuestionSubmission();
                questionSubmission.setCalculatedPoints(calculatedScore);
                questionSubmission.setAlteredGrade(null);
                questionSubmission.setSubmission(integrationToken.getSubmission());
                questionSubmission.setQuestion(
                    questionRepository.findByAssessment_AssessmentIdAndQuestionId(
                        integrationToken.getSubmission().getAssessment().getAssessmentId(),
                        integrationToken.getSubmission().getAssessment().getQuestions().get(0).getQuestionId()
                    )
                    .orElseThrow(
                        () -> new DataServiceException(
                            String.format(
                                "Question ID: [%s] does not exist or does not belong to the submission ID: [%s] and assessment ID: [%s]",
                                integrationToken.getSubmission().getAssessment().getQuestions().get(0).getQuestionId(),
                                integrationToken.getSubmission().getSubmissionId(),
                                integrationToken.getSubmission().getAssessment().getAssessmentId()
                            )
                        )
                    )
                );

                questionSubmissionRepository.save(questionSubmission);
            }

            integrationToken.getSubmission().setAlteredCalculatedGrade(calculatedScore);
            integrationToken.getSubmission().setCalculatedGrade(calculatedScore);
            integrationToken.getSubmission().setTotalAlteredGrade(calculatedScore);
            integrationToken.getSubmission().setDateSubmitted(Timestamp.from(Instant.now()));

            submissionRepository.save(integrationToken.getSubmission());

            // send event to Caliper id SecuredInfo is present
            if (integrationToken.getSecuredInfo().isPresent()) {
                caliperService.sendAssignmentSubmitted(integrationToken.getSubmission(), integrationToken.getSecuredInfo().get());
            }

            // send the grade to lms asyncronously
            integrationScoreAsyncService.sendGradeToLms(integrationToken.getSubmission().getSubmissionId(), true);

            // create a log for the token
            integrationTokenLogRepository.save(
                IntegrationTokenLog.builder()
                    .integrationToken(integrationToken)
                    .score(score)
                    .status(IntegrationTokenStatus.SUCCESS)
                    .build()
            );
        } catch (IntegrationTokenInvalidException e) {
            throw new IntegrationTokenInvalidException(
                createErrorLog(e.getMessage(), score, launchToken, IntegrationTokenStatus.INVALID),
                e
            );
        } catch (IntegrationTokenExpiredException e) {
            throw new IntegrationTokenExpiredException(
                createErrorLog(e.getMessage(), score, launchToken, IntegrationTokenStatus.EXPIRED),
                e
            );
        } catch (IntegrationTokenAlreadyRedeemedException e) {
            throw new IntegrationTokenAlreadyRedeemedException(
                createErrorLog(e.getMessage(), score, launchToken, IntegrationTokenStatus.ALREADY_REDEEMED),
                e
            );
        } catch (IntegrationTokenNotFoundException e) {
            throw new IntegrationTokenNotFoundException(
                createErrorLog(e.getMessage(), score, launchToken, IntegrationTokenStatus.NOT_FOUND),
                e
            );
        } catch (DataServiceException e) {
            throw new DataServiceException(
                createErrorLog(e.getMessage(), score, launchToken),
                e
            );
        } catch (Exception e) {
            throw new RuntimeException(
                createErrorLog(e.getMessage(), score, launchToken),
                e
            );
        }
    }

    private String createErrorLog(String errorMessage, String score, String launchToken) {
        return createErrorLog(errorMessage, score, launchToken, IntegrationTokenStatus.ERROR);
    }

    private String createErrorLog(String errorMessage, String score, String launchToken, IntegrationTokenStatus status) {
        String code = RandomStringUtils.secure().nextAlphanumeric(IntegrationTokenLog.ERROR_CODE_LENGTH);

        while(integrationTokenLogRepository.findByCode(code).isPresent()) {
            // code already exists in database; create another
            code = RandomStringUtils.secure().nextAlphanumeric(IntegrationTokenLog.ERROR_CODE_LENGTH);
        }

        integrationTokenLogRepository.save(
            IntegrationTokenLog.builder()
                .code(code)
                .error(errorMessage)
                .score(score)
                .status(status)
                .token(launchToken)
                .build()
        );

        return code;
    }

    @Override
    public Optional<String> getPreviewTokenClient(String launchToken) {
        if (StringUtils.isBlank(launchToken)) {
            return Optional.empty();
        }

        Optional<IntegrationClient> integrationClient = integrationClientRepository.findByPreviewToken(launchToken);

        if (integrationClient.isPresent()) {
            return Optional.of(integrationClient.get().getName());
        }

        return Optional.empty();
    }

}
