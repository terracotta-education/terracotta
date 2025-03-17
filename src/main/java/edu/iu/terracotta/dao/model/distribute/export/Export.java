package edu.iu.terracotta.dao.model.distribute.export;

import java.io.File;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

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
public class Export {

    public static final String EXPORT_FILE_NAME = "Terracotta_experiment_export_%s_(%s)";

    private List<AnswerMcExport> answersMc;
    private List<AssessmentExport> assessments;
    private List<AssignmentExport> assignments;
    private List<ConditionExport> conditions;
    private ConsentDocumentExport consentDocument;
    private ExperimentExport experiment;
    private List<ExposureGroupConditionExport> exposureGroupConditions;
    private List<ExposureExport> exposures;
    private List<GroupExport> groups;
    private List<IntegrationClientExport> integrationClients;
    private List<IntegrationConfigurationExport> integrationConfigurations;
    private List<IntegrationExport> integrations;
    private OriginExport origin;
    private List<OutcomeExport> outcomes;
    private List<QuestionExport> questions;
    private List<TreatmentExport> treatments;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private File importDirectory;

}
