package edu.iu.terracotta.service.app.integrations;

import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.integrations.Integration;

public interface IntegrationLaunchParameterService {

    String buildQueryString(Submission submission, int submissionCount);
    String buildPreviewQueryString(Integration integration);

}
