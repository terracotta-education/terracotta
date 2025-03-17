package edu.iu.terracotta.dao.model.dto;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.enums.MultipleSubmissionScoringScheme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentDto {

    @Builder.Default private String multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.MOST_RECENT.name();
    @Builder.Default private boolean allowStudentViewResponses = false;
    @Builder.Default private boolean allowStudentViewCorrectAnswers = false;

    private Long assignmentId;
    private Long exposureId;
    private String title;
    private String lmsAssignmentId;
    private Integer assignmentOrder;
    private String resourceLinkId;
    private boolean started;
    private Boolean softDeleted;
    private Integer numOfSubmissions;
    private Float hoursBetweenSubmissions;
    private Float cumulativeScoringInitialPercentage;
    private List<TreatmentDto> treatments;
    private Timestamp studentViewResponsesAfter;
    private Timestamp studentViewResponsesBefore;
    private Timestamp studentViewCorrectAnswersAfter;
    private Timestamp studentViewCorrectAnswersBefore;
    private boolean published;
    private Date dueDate;

}
