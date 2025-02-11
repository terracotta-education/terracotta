package edu.iu.terracotta.service.app.integrations.impl;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
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
    public void sendGradeToLms(long submissionId, boolean student) throws ApiException {
        Submission gradedSubmission = submissionRepository.findBySubmissionId(submissionId);

        try {
            submissionService.sendSubmissionGradeToLmsWithLti(gradedSubmission, true);
        } catch (DataServiceException | ConnectionException | IOException | ApiException | TerracottaConnectorException e) {
            log.error("Error syncing integration submission grade with the LMS. Submission ID: [{}]. Grade: [{}]", submissionId, gradedSubmission.getCalculatedGrade(), e);
        }
    }

}
