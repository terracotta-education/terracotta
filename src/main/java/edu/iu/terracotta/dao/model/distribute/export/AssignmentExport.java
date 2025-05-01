package edu.iu.terracotta.dao.model.distribute.export;

import java.sql.Timestamp;

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
public class AssignmentExport {

    private long id;
    private Timestamp studentViewResponsesAfter;
    private Timestamp studentViewResponsesBefore;
    private Timestamp studentViewCorrectAnswersAfter;
    private Timestamp studentViewCorrectAnswersBefore;
    private Float cumulativeScoringInitialPercentage;
    private String title;
    private Integer assignmentOrder;
    private Integer numOfSubmissions;
    private Float hoursBetweenSubmissions;
    private MultipleSubmissionScoringScheme multipleSubmissionScoringScheme;
    private boolean allowStudentViewResponses;
    private boolean allowStudentViewCorrectAnswers;
    private long exposureId;

}
