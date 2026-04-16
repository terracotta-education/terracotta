package edu.iu.terracotta.service.app.integrations.impl;

import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.service.app.integrations.IntegrationLaunchParameterService;
import edu.iu.terracotta.service.app.integrations.IntegrationLaunchService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IntegrationLaunchServiceImpl implements IntegrationLaunchService {

    private final IntegrationLaunchParameterService integrationLaunchParameterService;

    @Override
    public void buildUrl(Submission submission, int submissionCount, Integration integration) {
        if (integration == null) {
            return;
        }

        submission.setIntegrationLaunchUrl(
            String.format(
                "%s%s",
                integration.getConfiguration().getLaunchUrl(),
                integrationLaunchParameterService.buildQueryString(
                    submission,
                    submissionCount
                )
            )
        );
    }

}
