package edu.iu.terracotta.service.app.integrations.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.integrations.Integration;
import edu.iu.terracotta.model.app.integrations.enums.IntegrationLaunchParameter;
import edu.iu.terracotta.service.app.integrations.IntegrationLaunchParameterService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class IntegrationLaunchParameterServiceImpl implements IntegrationLaunchParameterService {

    @Override
    public String buildQueryString(Submission submission, int submissionCount) {
        Map<String, List<String>> parameterMap = EnumUtils.getEnumList(IntegrationLaunchParameter.class).stream()
            .collect(
                Collectors.toMap(
                    IntegrationLaunchParameter::key,
                    (integrationLaunchParameter) -> {
                        String value;

                        switch (integrationLaunchParameter) {
                            case ANONYMOUS_ID:
                                value = Long.toString(submission.getParticipant().getParticipantId());
                                break;
                            case ASSIGNMENT_ID:
                                value = submission.getAssessment().getTreatment().getAssignment().getLmsAssignmentId();
                                break;
                            case CONDITION_NAME:
                                value = submission.getAssessment().getTreatment().getCondition().getName();
                                break;
                            case DUE_AT:
                                String dueAt = "";

                                if (submission.getAssessment().getTreatment().getAssignment().getDueDate() != null) {
                                    dueAt = submission.getAssessment().getTreatment().getAssignment().getDueDate().toString();
                                }

                                value = dueAt;
                                break;
                            case EXPERIMENT_ID:
                                value = submission.getParticipant().getExperiment().getTitle();
                                break;
                            case LAUNCH_TOKEN:
                                value = submission.getLatestIntegrationToken().get().getToken();
                                break;
                            case REMAINING_ATTEMPTS:
                                value = submission.getIntegration().getQuestion().getAssessment().getNumOfSubmissions() == 0 ? Integration.UNLIMITED_ATTEMPTS_VALUE : Integer.toString(submission.getAssessment().getNumOfSubmissions() - submissionCount);
                                break;
                            case SUBMISSION_ID:
                                value = Long.toString(submission.getSubmissionId());
                                break;
                            default:
                                value = null;
                                break;
                        }

                        return Collections.singletonList(value);
                    }
                )
            );

        return buildUri(parameterMap);
    }

    @Override
    public String buildPreviewQueryString(Integration integration) {
        Map<String, List<String>> parameterMap = EnumUtils.getEnumList(IntegrationLaunchParameter.class).stream()
            .collect(
                Collectors.toMap(
                    IntegrationLaunchParameter::key,
                    (integrationLaunchParameter) -> {
                        String value;

                        switch (integrationLaunchParameter) {
                            case ANONYMOUS_ID:
                                value = Long.toString(integration.getQuestion().getAssessment().getTreatment().getCondition().getExperiment().getCreatedBy().getUserId());
                                break;
                            case ASSIGNMENT_ID:
                                value = integration.getQuestion().getAssessment().getTreatment().getAssignment().getLmsAssignmentId();
                                break;
                            case CONDITION_NAME:
                                value = integration.getQuestion().getAssessment().getTreatment().getCondition().getName();
                                break;
                            case DUE_AT:
                                String dueAt = "";

                                if (integration.getQuestion().getAssessment().getTreatment().getAssignment().getDueDate() != null) {
                                    dueAt = integration.getQuestion().getAssessment().getTreatment().getAssignment().getDueDate().toString();
                                }

                                value = dueAt;
                                break;
                            case EXPERIMENT_ID:
                                value = integration.getQuestion().getAssessment().getTreatment().getCondition().getExperiment().getTitle();
                                break;
                            case LAUNCH_TOKEN:
                                value = integration.getConfiguration().getClient().getPreviewToken();
                                break;
                            case REMAINING_ATTEMPTS:
                                value = integration.getQuestion().getAssessment().getNumOfSubmissions() == 0 ? Integration.UNLIMITED_ATTEMPTS_VALUE : Integer.toString(integration.getQuestion().getAssessment().getNumOfSubmissions() - 0);
                                break;
                            case SUBMISSION_ID:
                                value = "preview-submission_id";
                                break;
                            default:
                                value = null;
                                break;
                        }

                        return Collections.singletonList(value);
                    }
                )
            );

        return buildUri(parameterMap);
    }

    private String buildUri(Map<String, List<String>> parameterMap) {
        return UriComponentsBuilder.newInstance()
            .queryParams(
                CollectionUtils.toMultiValueMap(parameterMap)
            )
            .encode()
            .build()
            .toUriString();
    }

}
