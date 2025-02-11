package edu.iu.terracotta.service.app.integrations;

import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.integrations.Integration;

public interface IntegrationLaunchService {

    void buildUrl(Submission submission, int submissionCount, Integration integration);

}
