package edu.iu.terracotta.service.app.integrations;

public interface IntegrationScoreAsyncService {

    void sendGradeToCanvas(long submissionId, boolean student);

}
