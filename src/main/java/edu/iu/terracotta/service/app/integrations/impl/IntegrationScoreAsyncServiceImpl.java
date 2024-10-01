package edu.iu.terracotta.service.app.integrations.impl;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.repository.SubmissionRepository;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.integrations.IntegrationScoreAsyncService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class IntegrationScoreAsyncServiceImpl implements IntegrationScoreAsyncService {

    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private SubmissionService submissionService;

    @Async
    @Override
    @Transactional
    public void sendGradeToCanvas(long submissionId, boolean student) {
        Submission gradedSubmission = submissionRepository.findBySubmissionId(submissionId);

        try {
            log.info("Syncing integration submission grade with Canvas. Submission ID: [{}]. Grade: [{}]", submissionId, gradedSubmission.getCalculatedGrade());
            submissionService.sendSubmissionGradeToCanvasWithLTI(gradedSubmission, true);
        } catch (CanvasApiException | DataServiceException | ConnectionException | IOException e) {
            log.error("Error syncing integration submission grade with Canvas. Submission ID: [{}]. Grade: [{}]", submissionId, gradedSubmission.getCalculatedGrade(), e);
        }
    }

}
