package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.ConditionDto;
import edu.iu.terracotta.exceptions.ConditionsLockedException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.utils.TextConstants;

public class ConditionControllerTest extends BaseTest {

    // ConditionService has no mock in the BaseTest hierarchy, so it must be declared locally.
    @Mock private ConditionService conditionService;

    private ConditionController conditionController;

    @BeforeEach
    void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // Constructed manually (not @InjectMocks) because ApiJwtService has two type-matching
        // mock candidates in BaseServiceTest (apiJwtService and canvasApiJwtService), and
        // Mockito's constructor injection matches by type only, with no field-name tiebreak.
        conditionController = new ConditionController(conditionService, apiJwtService);

        when(apiJwtService.extractValues(any(), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(conditionService.findByConditionId(anyLong())).thenReturn(condition);
    }

    @Test
    void testAllConditionsByExperiment() throws Exception {
        ConditionDto dto = ConditionDto.builder().conditionId(1L).build();
        when(conditionService.findAllByExperimentId(1L)).thenReturn(List.of(dto));

        ResponseEntity<List<ConditionDto>> response = conditionController.allConditionsByExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testAllConditionsByExperimentNoContent() throws Exception {
        when(conditionService.findAllByExperimentId(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<ConditionDto>> response = conditionController.allConditionsByExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testAllConditionsByExperimentUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<ConditionDto>> response = conditionController.allConditionsByExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testAllConditionsByExperimentNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("error")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> conditionController.allConditionsByExperiment(1L, httpServletRequest));
    }

    @Test
    void testGetCondition() throws Exception {
        ConditionDto dto = ConditionDto.builder().conditionId(1L).build();
        when(conditionService.getCondition(1L)).thenReturn(dto);

        ResponseEntity<ConditionDto> response = conditionController.getCondition(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testGetConditionUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ConditionDto> response = conditionController.getCondition(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void testGetConditionNotMatching() throws Exception {
        doThrow(new ConditionNotMatchingException("error")).when(apiJwtService).conditionAllowed(securedInfo, 1L, 1L);

        assertThrows(ConditionNotMatchingException.class, () -> conditionController.getCondition(1L, 1L, httpServletRequest));
    }

    @Test
    void testPostCondition() throws Exception {
        ConditionDto requestDto = ConditionDto.builder().name("new condition").build();
        ConditionDto returnedDto = ConditionDto.builder().conditionId(1L).name("new condition").build();
        when(conditionService.postCondition(requestDto, 1L)).thenReturn(returnedDto);

        ResponseEntity<ConditionDto> response = conditionController.postCondition(1L, requestDto, httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(returnedDto, response.getBody());
    }

    @Test
    void testPostConditionNullBody() throws Exception {
        ConditionDto returnedDto = ConditionDto.builder().conditionId(1L).build();
        when(conditionService.postCondition(any(ConditionDto.class), anyLong())).thenReturn(returnedDto);

        ResponseEntity<ConditionDto> response = conditionController.postCondition(1L, null, httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(returnedDto, response.getBody());
    }

    @Test
    void testPostConditionUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ConditionDto> response = conditionController.postCondition(1L, ConditionDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testPostConditionLocked() throws Exception {
        doThrow(new ExperimentLockedException("error")).when(apiJwtService).experimentLocked(1L, true);

        assertThrows(ExperimentLockedException.class, () -> conditionController.postCondition(1L, ConditionDto.builder().build(), httpServletRequest));
    }

    @Test
    void testUpdateCondition() throws Exception {
        ResponseEntity<Void> response = conditionController.updateCondition(1L, 1L, ConditionDto.builder().name("updated").build(), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(conditionService, times(1)).updateCondition(any());
    }

    @Test
    void testUpdateConditionUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = conditionController.updateCondition(1L, 1L, ConditionDto.builder().name("updated").build(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUpdateConditionNotMatching() throws Exception {
        doThrow(new ConditionNotMatchingException("error")).when(apiJwtService).conditionAllowed(securedInfo, 1L, 1L);

        assertThrows(ConditionNotMatchingException.class, () -> conditionController.updateCondition(1L, 1L, ConditionDto.builder().name("updated").build(), httpServletRequest));
    }

    @Test
    void testUpdateConditionTitleValidation() throws Exception {
        doThrow(new TitleValidationException("error")).when(conditionService).validateConditionName(any(), any(), anyLong(), anyLong(), anyBoolean());

        assertThrows(TitleValidationException.class, () -> conditionController.updateCondition(1L, 1L, ConditionDto.builder().name("updated").build(), httpServletRequest));
    }

    @Test
    void testUpdateConditions() throws Exception {
        ConditionDto dto = ConditionDto.builder().conditionId(1L).name("updated").build();

        ResponseEntity<Void> response = conditionController.updateConditions(1L, List.of(dto), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(apiJwtService, times(1)).conditionAllowed(securedInfo, 1L, 1L);
    }

    @Test
    void testUpdateConditionsUnauthorized() throws Exception {
        // Note: apijwtService.isInstructorOrHigher is checked AFTER conditionService.validateConditionNames
        // runs, so validation still executes for an unauthorized caller before the 401 is returned.
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        ConditionDto dto = ConditionDto.builder().conditionId(1L).name("updated").build();

        ResponseEntity<Void> response = conditionController.updateConditions(1L, List.of(dto), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(conditionService, times(1)).validateConditionNames(List.of(dto), 1L, true);
    }

    @Test
    void testUpdateConditionsTitleValidation() throws Exception {
        doThrow(new TitleValidationException("error")).when(conditionService).validateConditionNames(any(), anyLong(), anyBoolean());
        ConditionDto dto = ConditionDto.builder().conditionId(1L).name("updated").build();

        assertThrows(TitleValidationException.class, () -> conditionController.updateConditions(1L, List.of(dto), httpServletRequest));
    }

    @Test
    void testUpdateConditionsDataServiceException() throws Exception {
        doThrow(new RuntimeException("boom")).when(conditionService).updateCondition(any());
        ConditionDto dto = ConditionDto.builder().conditionId(1L).name("updated").build();

        assertThrows(DataServiceException.class, () -> conditionController.updateConditions(1L, List.of(dto), httpServletRequest));
    }

    @Test
    void testDeleteCondition() throws Exception {
        when(conditionService.isDefaultCondition(1L)).thenReturn(false);

        ResponseEntity<Void> response = conditionController.deleteCondition(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteConditionDefaultConflict() throws Exception {
        when(conditionService.isDefaultCondition(1L)).thenReturn(true);

        ResponseEntity<Void> response = conditionController.deleteCondition(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testDeleteConditionNotFound() throws Exception {
        when(conditionService.isDefaultCondition(1L)).thenReturn(false);
        doThrow(new EmptyResultDataAccessException(1)).when(conditionService).deleteById(1L);

        ResponseEntity<Void> response = conditionController.deleteCondition(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDeleteConditionUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = conditionController.deleteCondition(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testDeleteConditionLocked() throws Exception {
        doThrow(new ConditionsLockedException("error")).when(apiJwtService).conditionsLocked(1L, true);

        assertThrows(ConditionsLockedException.class, () -> conditionController.deleteCondition(1L, 1L, httpServletRequest));
    }

}
