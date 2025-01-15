package edu.iu.terracotta.service.app.impl;

import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.RegradeDetails;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.enumerator.RegradeOption;
import edu.iu.terracotta.repository.AnswerMcSubmissionRepository;
import edu.iu.terracotta.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.repository.SubmissionRepository;
import edu.iu.terracotta.service.app.AssessmentSubmissionService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;

@Service
public class AssessmentSubmissionServiceImpl implements AssessmentSubmissionService {

    @Autowired private AnswerMcSubmissionRepository answerMcSubmissionRepository;
    @Autowired private QuestionSubmissionRepository questionSubmissionRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private QuestionSubmissionService questionSubmissionService;

    @Override
    public Submission gradeSubmission(Submission submission, RegradeDetails regradeDetails) throws DataServiceException {
        //We need to calculate the 2 the possible grades. Automatic and manual
        float automatic = 0f;
        float manual = 0f;

        for (QuestionSubmission questionSubmission : submission.getQuestionSubmissions()) {
            // If multiple choice, we take the automatic score for automatic and the manual if any for manual, and if no manual, then the automatic for manual
            QuestionSubmission questionGraded = questionSubmission;

            switch (questionSubmission.getQuestion().getQuestionType()) {
                case MC:
                    List<AnswerMcSubmission> answerMcSubmissions = answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId());

                    if (answerMcSubmissions.size() == 1) {
                        switch (regradeDetails.getRegradeOption()) {
                            case FULL:
                                if (!isRegradeEligible(regradeDetails.getEditedMCQuestionIds(), questionSubmission.getQuestion().getQuestionId())) {
                                    // this is not an edited question submission; skip regrading
                                    break;
                                }

                                // set score to full max points
                                questionSubmission.setCalculatedPoints(questionSubmission.getQuestion().getPoints());
                                questionSubmission.setAlteredGrade(null);
                                questionGraded = questionSubmissionRepository.save(questionSubmission);
                                break;

                            case BOTH:
                                if (!isRegradeEligible(regradeDetails.getEditedMCQuestionIds(), questionSubmission.getQuestion().getQuestionId())) {
                                    // this is not an edited question submission; skip regrading
                                    break;
                                }

                                // current answer is true or has previously-added points; set to max question points (in case point value changed)
                                if (BooleanUtils.isTrue(answerMcSubmissions.get(0).getAnswerMc().getCorrect())
                                        || questionSubmission.getCalculatedPoints() != null && questionSubmission.getCalculatedPoints() > 0) {
                                    //
                                    questionSubmission.setCalculatedPoints(questionSubmission.getQuestion().getPoints());
                                }

                                questionSubmission.setAlteredGrade(null);
                                questionGraded = questionSubmissionRepository.save(questionSubmission);
                                break;

                            case CURRENT:
                                if (!isRegradeEligible(regradeDetails.getEditedMCQuestionIds(), questionSubmission.getQuestion().getQuestionId())) {
                                    // this is not an edited question submission; skip regrading
                                    questionGraded = questionSubmission;
                                    break;
                                }

                                questionSubmission.setAlteredGrade(null);
                                questionGraded = questionSubmissionService.automaticGradingMC(questionSubmission, answerMcSubmissions.get(0));
                                break;

                            case NA:
                                if (RegradeOption.FULL == questionSubmission.getQuestion().getRegradeOption()) {
                                    // regrade FULL option previously selected; set score to full max points
                                    questionSubmission.setCalculatedPoints(questionSubmission.getQuestion().getPoints());
                                    questionSubmission.setAlteredGrade(null);
                                    questionGraded = questionSubmissionRepository.save(questionSubmission);
                                    break;
                                }

                                questionGraded = questionSubmissionService.automaticGradingMC(questionSubmission, answerMcSubmissions.get(0));
                                break;

                            case NONE:
                            default:
                                questionGraded = questionSubmission;
                                break;
                        }
                    } else if (answerMcSubmissions.size() > 1) {
                        throw new DataServiceException("Error 135: Cannot have more than one answer submission for a multiple choice question.");
                    } else {
                        questionGraded.setCalculatedPoints(0f);
                    }

                    break;
                case ESSAY:
                case FILE:
                case INTEGRATION:
                    questionGraded = questionSubmission;

                    if (RegradeOption.NA == regradeDetails.getRegradeOption()) {
                        // not a regrade
                        questionGraded.setCalculatedPoints(0f);
                    }

                    break;
                default:
                    break;
            }

            automatic = automatic + (questionGraded.getCalculatedPoints() != null ? questionGraded.getCalculatedPoints() : 0f);

            if (questionGraded.getAlteredGrade() != null && !questionGraded.getAlteredGrade().isNaN()) {
                manual = manual + questionGraded.getAlteredGrade();
            } else {
                manual = manual + (questionGraded.getCalculatedPoints() != null ? questionGraded.getCalculatedPoints() : 0f);
            }
            // TODO: If open question, we take the manual score for both, because the automatic will be always 0
        }

        submission.setCalculatedGrade(automatic);
        submission.setAlteredCalculatedGrade(manual);

        if (!isGradeAltered(submission) || RegradeOption.NA != regradeDetails.getRegradeOption()) {
            // grade is not altered or this is a regrade
            submission.setTotalAlteredGrade(manual);
        }

        return submissionRepository.save(submission);
    }

    private boolean isRegradeEligible(List<Long> editedMCQuestionIds, long questionId) {
        return editedMCQuestionIds.contains(questionId);
    }

    @Override
    public boolean isGradeAltered(Submission submission) {
        Float totalAlteredGrade = submission.getTotalAlteredGrade();

        return totalAlteredGrade != null && !totalAlteredGrade.equals(submission.getAlteredCalculatedGrade());
    }

    @Override
    public Float calculateMaxScore(Assessment assessment) {
        float score = 0f;

        for (Question question : assessment.getQuestions()) {
            score += question.getPoints();
        }

        return score;
    }

}
