package edu.iu.terracotta.service.app.integrations;

import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.integrations.Integration;

public interface IntegrationLaunchParameterService {

    String buildQueryString(Submission submission, int submissionCount);
    String buildPreviewQueryString(Integration integration);

}
