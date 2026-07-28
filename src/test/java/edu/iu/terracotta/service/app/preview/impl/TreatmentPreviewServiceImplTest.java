package edu.iu.terracotta.service.app.preview.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.preview.TreatmentPreview;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.SubmissionDto;
import edu.iu.terracotta.dao.model.dto.preview.TreatmentPreviewDto;
import edu.iu.terracotta.dao.repository.preview.TreatmentPreviewRepository;

class TreatmentPreviewServiceImplTest extends BaseTest {

    @Mock private TreatmentPreviewRepository treatmentPreviewRepository;
    @Mock private TreatmentPreview treatmentPreview;

    @InjectMocks private TreatmentPreviewServiceImpl treatmentPreviewService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
    }

    @Test
    void testCreateSuccess() {
        when(experimentRepository.findByExperimentId(1L)).thenReturn(experiment);
        when(conditionRepository.findByConditionId(2L)).thenReturn(condition);
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment("owner-id", platformDeployment)).thenReturn(ltiUserEntity);
        when(treatmentRepository.findByTreatmentId(3L)).thenReturn(treatment);
        when(treatmentPreviewRepository.save(any(TreatmentPreview.class))).thenReturn(treatmentPreview);

        TreatmentPreview result = treatmentPreviewService.create(3L, 1L, 2L, "owner-id");

        assertEquals(treatmentPreview, result);
        ArgumentCaptor<TreatmentPreview> captor = ArgumentCaptor.forClass(TreatmentPreview.class);
        verify(treatmentPreviewRepository).save(captor.capture());
        assertEquals(experiment, captor.getValue().getExperiment());
        assertEquals(condition, captor.getValue().getCondition());
        assertEquals(treatment, captor.getValue().getTreatment());
        assertEquals(ltiUserEntity, captor.getValue().getOwner());
    }

    @Test
    void testGetTreatmentPreviewNotFound() {
        UUID uuid = UUID.randomUUID();
        when(treatmentPreviewRepository.findByUuidAndTreatment_TreatmentIdAndExperiment_ExperimentIdAndCondition_ConditionIdAndOwner_UserKey(uuid, 3L, 1L, 2L, "owner-id"))
            .thenReturn(Optional.empty());

        TreatmentNotMatchingException exception = assertThrows(
            TreatmentNotMatchingException.class,
            () -> treatmentPreviewService.getTreatmentPreview(uuid, 3L, 1L, 2L, "owner-id", securedInfo)
        );

        assertEquals(
            String.format(
                "No treatment preview found for uuid: [%s] and treatment ID: [%s] and experiment ID: [%s] and condition ID: [%s] and owner ID: [%s]",
                uuid,
                3L,
                1L,
                2L,
                "owner-id"
            ),
            exception.getMessage()
        );
    }

    @Test
    void testGetTreatmentPreviewSuccessSingleQuestion() throws TreatmentNotMatchingException, AssessmentNotMatchingException {
        UUID uuid = UUID.randomUUID();
        when(treatmentPreviewRepository.findByUuidAndTreatment_TreatmentIdAndExperiment_ExperimentIdAndCondition_ConditionIdAndOwner_UserKey(uuid, 3L, 1L, 2L, "owner-id"))
            .thenReturn(Optional.of(treatmentPreview));
        when(treatmentPreview.getUuid()).thenReturn(uuid);
        when(treatmentPreview.getTreatment()).thenReturn(treatment);
        when(treatmentDto.getAssessmentDto()).thenReturn(assessmentDto);
        when(questionService.toDto(List.of(question), true, true)).thenReturn(List.of(questionDto));
        when(answerService.findAllByQuestionIdMC(1L, true)).thenReturn(List.of(answerDto));

        TreatmentPreviewDto result = treatmentPreviewService.getTreatmentPreview(uuid, 3L, 1L, 2L, "owner-id", securedInfo);

        assertEquals(uuid, result.getId());
        assertEquals(treatmentDto, result.getTreatment());
        verify(assessmentDto).setQuestions(List.of(questionDto));

        SubmissionDto submissionDto = result.getSubmission();
        assertEquals(1L, submissionDto.getAssessmentId());
        assertEquals(1L, submissionDto.getConditionId());
        assertEquals(1L, submissionDto.getExperimentId());
        assertEquals(1L, submissionDto.getSubmissionId());
        assertEquals(3L, submissionDto.getTreatmentId());
        assertEquals(1, submissionDto.getQuestionSubmissionDtoList().size());
        assertEquals(1L, submissionDto.getQuestionSubmissionDtoList().get(0).getQuestionSubmissionId());
        assertEquals(1L, submissionDto.getQuestionSubmissionDtoList().get(0).getQuestionId());
        assertEquals(List.of(answerDto), submissionDto.getQuestionSubmissionDtoList().get(0).getAnswerDtoList());
    }

    @Test
    void testGetTreatmentPreviewSuccessMultipleQuestionsIncrementsSubmissionId() throws TreatmentNotMatchingException, AssessmentNotMatchingException {
        UUID uuid = UUID.randomUUID();
        Question secondQuestion = mock(Question.class);
        when(secondQuestion.getQuestionId()).thenReturn(2L);
        when(assessment.getQuestions()).thenReturn(List.of(question, secondQuestion));
        when(treatmentPreviewRepository.findByUuidAndTreatment_TreatmentIdAndExperiment_ExperimentIdAndCondition_ConditionIdAndOwner_UserKey(uuid, 3L, 1L, 2L, "owner-id"))
            .thenReturn(Optional.of(treatmentPreview));
        when(treatmentPreview.getUuid()).thenReturn(uuid);
        when(treatmentPreview.getTreatment()).thenReturn(treatment);
        when(treatmentDto.getAssessmentDto()).thenReturn(assessmentDto);
        when(questionService.toDto(List.of(question, secondQuestion), true, true)).thenReturn(List.of(questionDto, questionDto));
        when(answerService.findAllByQuestionIdMC(anyLong(), eq(true))).thenReturn(List.of(answerDto));

        TreatmentPreviewDto result = treatmentPreviewService.getTreatmentPreview(uuid, 3L, 1L, 2L, "owner-id", securedInfo);

        List<edu.iu.terracotta.dao.model.dto.QuestionSubmissionDto> questionSubmissions = result.getSubmission().getQuestionSubmissionDtoList();
        assertEquals(2, questionSubmissions.size());
        assertEquals(1L, questionSubmissions.get(0).getQuestionSubmissionId());
        assertEquals(1L, questionSubmissions.get(0).getQuestionId());
        assertEquals(2L, questionSubmissions.get(1).getQuestionSubmissionId());
        assertEquals(2L, questionSubmissions.get(1).getQuestionId());
    }

}
