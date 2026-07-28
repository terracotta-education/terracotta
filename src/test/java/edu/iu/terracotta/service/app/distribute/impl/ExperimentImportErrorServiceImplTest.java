package edu.iu.terracotta.service.app.distribute.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.model.dto.distribute.ExperimentImportErrorDto;

class ExperimentImportErrorServiceImplTest extends BaseTest {

    @InjectMocks private ExperimentImportErrorServiceImpl experimentImportErrorService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
    }

    @Test
    void testToDtoSingle() {
        UUID errorUuid = UUID.randomUUID();
        UUID importUuid = UUID.randomUUID();
        when(experimentImportError.getUuid()).thenReturn(errorUuid);
        when(experimentImportError.getText()).thenReturn("something went wrong");
        when(experimentImportError.getExperimentImport()).thenReturn(experimentImport);
        when(experimentImport.getUuid()).thenReturn(importUuid);

        ExperimentImportErrorDto result = experimentImportErrorService.toDto(experimentImportError);

        assertEquals(errorUuid, result.getId());
        assertEquals("something went wrong", result.getText());
        assertEquals(importUuid, result.getExperimentImportId());
    }

    @Test
    void testToDtoList() {
        UUID errorUuid = UUID.randomUUID();
        UUID importUuid = UUID.randomUUID();
        when(experimentImportError.getUuid()).thenReturn(errorUuid);
        when(experimentImportError.getText()).thenReturn("something went wrong");
        when(experimentImportError.getExperimentImport()).thenReturn(experimentImport);
        when(experimentImport.getUuid()).thenReturn(importUuid);

        List<ExperimentImportErrorDto> result = experimentImportErrorService.toDto(List.of(experimentImportError, experimentImportError));

        assertEquals(2, result.size());
        assertEquals(errorUuid, result.get(0).getId());
        assertEquals(errorUuid, result.get(1).getId());
    }

    @Test
    void testToDtoListEmpty() {
        List<ExperimentImportErrorDto> result = experimentImportErrorService.toDto(Collections.emptyList());

        assertTrue(result.isEmpty());
    }

}
