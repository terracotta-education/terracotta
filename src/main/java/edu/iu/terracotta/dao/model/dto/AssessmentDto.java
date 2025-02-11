package edu.iu.terracotta.dao.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import edu.iu.terracotta.dao.entity.RetakeDetails;
import edu.iu.terracotta.dao.model.dto.integrations.IntegrationClientDto;
import edu.iu.terracotta.dao.model.enums.MultipleSubmissionScoringScheme;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentDto {

    private Long assessmentId;
    private String html;
    private Long treatmentId;
    private boolean autoSubmit;
    private Integer numOfSubmissions;
    private Float hoursBetweenSubmissions;
    private String multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.MOST_RECENT.name();
    private Float cumulativeScoringInitialPercentage;
    private Float maxPoints;
    private boolean started;
    private Long submissionsExpected;
    private Long submissionsCompletedCount;
    private Long submissionsInProgressCount;
    private boolean allowStudentViewResponses = false;
    private Timestamp studentViewResponsesAfter;
    private Timestamp studentViewResponsesBefore;
    private boolean allowStudentViewCorrectAnswers = false;
    private Timestamp studentViewCorrectAnswersAfter;
    private Timestamp studentViewCorrectAnswersBefore;
    private RetakeDetails retakeDetails;
    private boolean isIntegration;
    private String integrationPreviewUrl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SubmissionDto> submissions;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<QuestionDto> questions;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<IntegrationClientDto> integrationClients;

}
