package edu.iu.terracotta.service.app.preview.impl;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.preview.TreatmentPreview;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionDto;
import edu.iu.terracotta.dao.model.dto.SubmissionDto;
import edu.iu.terracotta.dao.model.dto.TreatmentDto;
import edu.iu.terracotta.dao.model.dto.preview.TreatmentPreviewDto;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.dao.repository.preview.TreatmentPreviewRepository;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.AssignmentTreatmentService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.service.app.preview.TreatmentPreviewService;

@Service
public class TreatmentPreviewServiceImpl implements TreatmentPreviewService {

    @Autowired private ConditionRepository conditionRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private TreatmentPreviewRepository treatmentPreviewRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private AnswerService answerService;
    @Autowired private AssignmentTreatmentService assignmentTreatmentService;
    @Autowired private QuestionService questionService;

    @Override
    public TreatmentPreview create(long treatmentId, long experimentId, long conditionId, String ownerId) {
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);

        return treatmentPreviewRepository.save(
            TreatmentPreview.builder()
                .condition(conditionRepository.findByConditionId(conditionId))
                .experiment(experiment)
                .owner(ltiUserRepository.findByUserKeyAndPlatformDeployment(ownerId, experiment.getPlatformDeployment()))
                .treatment(treatmentRepository.findByTreatmentId(treatmentId))
                .build());
    }

    @Override
    public TreatmentPreviewDto getTreatmentPreview(UUID uuid, long treatmentId, long experimentId, long conditionId, String ownerId) throws TreatmentNotMatchingException, AssessmentNotMatchingException {
        TreatmentPreview treatmentPreview = treatmentPreviewRepository.findByUuidAndTreatment_TreatmentIdAndExperiment_ExperimentIdAndCondition_ConditionIdAndOwner_UserKey(uuid, treatmentId, experimentId, conditionId, ownerId)
            .orElseThrow(() -> new TreatmentNotMatchingException(
                String.format(
                    "No treatment preview found for uuid: [%s] and treatment ID: [%s] and experiment ID: [%s] and condition ID: [%s] and owner ID: [%s]",
                    uuid,
                    treatmentId,
                    experimentId,
                    conditionId,
                    ownerId
                )
            )
        );

        TreatmentDto treatmentDto = assignmentTreatmentService.toTreatmentDto(treatmentPreview.getTreatment(), false, false);
        treatmentDto.getAssessmentDto().setQuestions(
            questionService.toDto(
                treatmentPreview.getTreatment().getAssessment().getQuestions(),
                true,
                true
            )
        );
        AtomicLong questionSubmissionId = new AtomicLong(1L);

        SubmissionDto submissionDto = SubmissionDto.builder()
            .assessmentId(treatmentPreview.getTreatment().getAssessment().getAssessmentId())
            .conditionId(treatmentPreview.getTreatment().getCondition().getConditionId())
            .experimentId(treatmentPreview.getTreatment().getCondition().getExperiment().getExperimentId())
            .questionSubmissionDtoList(
                treatmentPreview.getTreatment().getAssessment().getQuestions().stream()
                    .map(question -> QuestionSubmissionDto.builder()
                        .answerDtoList(answerService.findAllByQuestionIdMC(question.getQuestionId(), true))
                        .questionSubmissionId(questionSubmissionId.getAndIncrement())
                        .questionId(question.getQuestionId())
                        .build()
                    )
                    .toList()
            )
            .submissionId(1L)
            .treatmentId(treatmentId)
            .build();

        return TreatmentPreviewDto.builder()
            .id(treatmentPreview.getUuid())
            .treatment(treatmentDto)
            .submission(submissionDto)
            .build();
    }

}
