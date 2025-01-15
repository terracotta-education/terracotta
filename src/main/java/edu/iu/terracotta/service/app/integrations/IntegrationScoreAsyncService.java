package edu.iu.terracotta.service.app.integrations;

import edu.iu.terracotta.connectors.generic.exceptions.ApiException;

public interface IntegrationScoreAsyncService {

    void sendGradeToLms(long submissionId, boolean student) throws ApiException;

}
