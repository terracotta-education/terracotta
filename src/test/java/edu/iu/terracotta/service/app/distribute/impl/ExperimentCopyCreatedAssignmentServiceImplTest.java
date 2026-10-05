package edu.iu.terracotta.service.app.distribute.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCandidate;
import edu.iu.terracotta.dao.entity.distribute.ExperimentCopyCreatedAssignment;
import edu.iu.terracotta.dao.repository.distribute.ExperimentCopyCandidateRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentCopyCreatedAssignmentRepository;

public class ExperimentCopyCreatedAssignmentServiceImplTest {

    @Mock private ExperimentCopyCandidateRepository experimentCopyCandidateRepository;
    @Mock private ExperimentCopyCreatedAssignmentRepository experimentCopyCreatedAssignmentRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ExperimentCopyCandidate copyCandidate;

    private ExperimentCopyCreatedAssignmentServiceImpl experimentCopyCreatedAssignmentService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        when(experimentCopyCandidateRepository.getReferenceById(5L)).thenReturn(copyCandidate);

        experimentCopyCreatedAssignmentService = new ExperimentCopyCreatedAssignmentServiceImpl(experimentCopyCandidateRepository, experimentCopyCreatedAssignmentRepository, transactionManager);
    }

    // it's called from inside the import's transaction, and has to outlive that transaction
    // being rolled back
    @Test
    void testRecordCreatedSavesInATransactionOfItsOwn() {
        experimentCopyCreatedAssignmentService.recordCreated(5L, "444");

        ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definition.capture());
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getValue().getPropagationBehavior());

        ArgumentCaptor<ExperimentCopyCreatedAssignment> saved = ArgumentCaptor.forClass(ExperimentCopyCreatedAssignment.class);
        verify(experimentCopyCreatedAssignmentRepository).save(saved.capture());
        assertEquals(copyCandidate, saved.getValue().getCopyCandidate());
        assertEquals("444", saved.getValue().getLmsAssignmentId());
    }

    @Test
    void testRecordCreatedFailureIsLoggedNotThrown() {
        when(experimentCopyCreatedAssignmentRepository.save(any())).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> experimentCopyCreatedAssignmentService.recordCreated(5L, "444"));
    }

    @Test
    void testClearDeletesTheRecordedAssignments() {
        List<ExperimentCopyCreatedAssignment> recorded = List.of(mock(ExperimentCopyCreatedAssignment.class));
        when(experimentCopyCreatedAssignmentRepository.findAllByCopyCandidate_Id(5L)).thenReturn(recorded);

        experimentCopyCreatedAssignmentService.clear(5L);

        verify(experimentCopyCreatedAssignmentRepository).deleteAll(recorded);
    }

    @Test
    void testClearFailureIsLoggedNotThrown() {
        when(experimentCopyCreatedAssignmentRepository.findAllByCopyCandidate_Id(5L)).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> experimentCopyCreatedAssignmentService.clear(5L));
    }

}
