package edu.iu.terracotta.service.app.integrations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.integrations.impl.IntegrationScoreAsyncServiceImpl;

public class IntegrationScoreAsyncServiceImplTest extends BaseTest {

    @InjectMocks private IntegrationScoreAsyncServiceImpl integrationScoreAsyncService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    void testSendGradeToLms() throws ConnectionException, DataServiceException, ApiException, IOException, TerracottaConnectorException {
        integrationScoreAsyncService.sendGradeToLms(1, false);

        verify(submissionService).sendSubmissionGradeToLmsWithLti(any(Submission.class), anyBoolean());
    }

    @Test
    void testSendGradeToLmsException() throws ConnectionException, DataServiceException, ApiException, IOException, TerracottaConnectorException {
        doThrow(new ApiException("")).when(submissionService).sendSubmissionGradeToLmsWithLti(any(Submission.class), anyBoolean());
        integrationScoreAsyncService.sendGradeToLms(1, false);
    }

}
