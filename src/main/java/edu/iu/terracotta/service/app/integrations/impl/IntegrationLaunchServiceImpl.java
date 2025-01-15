package edu.iu.terracotta.service.app.integrations.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.integrations.Integration;
import edu.iu.terracotta.service.app.integrations.IntegrationLaunchParameterService;
import edu.iu.terracotta.service.app.integrations.IntegrationLaunchService;

@Service
public class IntegrationLaunchServiceImpl implements IntegrationLaunchService {

    @Autowired private IntegrationLaunchParameterService integrationLaunchParameterService;

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
