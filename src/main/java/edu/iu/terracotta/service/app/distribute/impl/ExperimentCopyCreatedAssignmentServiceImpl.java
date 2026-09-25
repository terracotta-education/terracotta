package edu.iu.terracotta.service.app.distribute.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCreatedAssignment;
import edu.iu.terracotta.dao.repository.distribute.ExperimentCopyCandidateRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentCopyCreatedAssignmentRepository;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyCreatedAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement")
public class ExperimentCopyCreatedAssignmentServiceImpl implements ExperimentCopyCreatedAssignmentService {

    private final ExperimentCopyCandidateRepository experimentCopyCandidateRepository;
    private final ExperimentCopyCreatedAssignmentRepository experimentCopyCreatedAssignmentRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void recordCreated(long copyCandidateId, String lmsAssignmentId) {
        try {
            // committed on its own, right away - it's called from inside the import's transaction,
            // and has to outlive that transaction being rolled back
            newTransaction().executeWithoutResult(transactionStatus ->
                experimentCopyCreatedAssignmentRepository.save(
                    ExperimentCopyCreatedAssignment.builder()
                        .copyCandidate(experimentCopyCandidateRepository.getReferenceById(copyCandidateId))
                        .lmsAssignmentId(lmsAssignmentId)
                        .build()
                )
            );
        } catch (Exception e) {
            log.error("Error recording LMS assignment ID: [{}] as created for copy candidate ID: [{}]", lmsAssignmentId, copyCandidateId, e);
        }
    }

    @Override
    public void clear(long copyCandidateId) {
        try {
            newTransaction().executeWithoutResult(transactionStatus ->
                experimentCopyCreatedAssignmentRepository.deleteAll(experimentCopyCreatedAssignmentRepository.findAllByCopyCandidate_Id(copyCandidateId))
            );
        } catch (Exception e) {
            log.error("Error clearing the created LMS assignments recorded for copy candidate ID: [{}]", copyCandidateId, e);
        }
    }

    private TransactionTemplate newTransaction() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return transactionTemplate;
    }

}
