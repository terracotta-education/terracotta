package edu.iu.terracotta.service.app.integrations;

import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.integrations.Integration;

public interface IntegrationLaunchService {

    void buildUrl(Submission submission, int submissionCount, Integration integration);

}
