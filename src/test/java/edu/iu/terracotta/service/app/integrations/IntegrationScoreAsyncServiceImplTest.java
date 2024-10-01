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
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.service.app.integrations.impl.IntegrationScoreAsyncServiceImpl;

public class IntegrationScoreAsyncServiceImplTest extends BaseTest {

    @InjectMocks private IntegrationScoreAsyncServiceImpl integrationScoreAsyncService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    void testSendGradeToCanvas() throws ConnectionException, DataServiceException, CanvasApiException, IOException {
        integrationScoreAsyncService.sendGradeToCanvas(1, false);

        verify(submissionService).sendSubmissionGradeToCanvasWithLTI(any(Submission.class), anyBoolean());
    }

    @Test
    void testSendGradeToCanvasException() throws ConnectionException, DataServiceException, CanvasApiException, IOException {
        doThrow(new CanvasApiException("")).when(submissionService).sendSubmissionGradeToCanvasWithLTI(any(Submission.class), anyBoolean());
        integrationScoreAsyncService.sendGradeToCanvas(1, false);
    }

}
