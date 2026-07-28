package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.TreatmentDto;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.utils.TextConstants;
import jakarta.servlet.http.HttpServletRequest;

public class TreatmentControllerTest extends BaseTest {

    private TreatmentController treatmentController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        treatmentController = new TreatmentController(apiJwtService, assignmentTreatmentService, treatmentService);

        when(apiJwtService.extractValues(any(HttpServletRequest.class), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(treatmentDto.getTreatmentId()).thenReturn(1L);
        when(treatmentService.getTreatment(anyLong())).thenReturn(treatment);
    }

    @Test
    void allTreatmentsByConditionTest() throws Exception {
        when(treatmentService.getTreatments(anyLong(), anyBoolean(), any(SecuredInfo.class))).thenReturn(List.of(treatmentDto));

        ResponseEntity<List<TreatmentDto>> ret = treatmentController.allTreatmentsByCondition(1L, 1L, false, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(1, ret.getBody().size());
    }

    @Test
    void allTreatmentsByConditionNoContentTest() throws Exception {
        when(treatmentService.getTreatments(anyLong(), anyBoolean(), any(SecuredInfo.class))).thenReturn(Collections.emptyList());

        ResponseEntity<List<TreatmentDto>> ret = treatmentController.allTreatmentsByCondition(1L, 1L, false, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, ret.getStatusCode());
    }

    @Test
    void allTreatmentsByConditionUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<List<TreatmentDto>> ret = treatmentController.allTreatmentsByCondition(1L, 1L, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void allTreatmentsByConditionExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("experiment not matching")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(ExperimentNotMatchingException.class, () -> treatmentController.allTreatmentsByCondition(1L, 1L, false, httpServletRequest));
    }

    @Test
    void getTreatmentTest() throws Exception {
        ResponseEntity<TreatmentDto> ret = treatmentController.getTreatment(1L, 1L, 1L, false, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(treatmentDto, ret.getBody());
    }

    @Test
    void getTreatmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<TreatmentDto> ret = treatmentController.getTreatment(1L, 1L, 1L, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void getTreatmentNotMatchingTest() throws Exception {
        doThrow(new TreatmentNotMatchingException("treatment not matching")).when(apiJwtService).treatmentAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong());

        assertThrows(TreatmentNotMatchingException.class, () -> treatmentController.getTreatment(1L, 1L, 1L, false, httpServletRequest));
    }

    @Test
    void postTreatmentTest() throws Exception {
        when(treatmentService.postTreatment(any(TreatmentDto.class), anyLong(), any(SecuredInfo.class))).thenReturn(treatmentDto);
        when(treatmentService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), anyLong())).thenReturn(new HttpHeaders());

        ResponseEntity<TreatmentDto> ret = treatmentController.postTreatment(1L, 1L, treatmentDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(treatmentDto, ret.getBody());
    }

    @Test
    void postTreatmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<TreatmentDto> ret = treatmentController.postTreatment(1L, 1L, treatmentDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void postTreatmentConditionNotMatchingTest() throws Exception {
        doThrow(new ConditionNotMatchingException("condition not matching")).when(apiJwtService).conditionAllowed(any(SecuredInfo.class), anyLong(), anyLong());

        assertThrows(ConditionNotMatchingException.class, () -> treatmentController.postTreatment(1L, 1L, treatmentDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void postTreatmentIdInPostTest() throws Exception {
        doThrow(new IdInPostException("id in post")).when(treatmentService).postTreatment(any(TreatmentDto.class), anyLong(), any(SecuredInfo.class));

        assertThrows(IdInPostException.class, () -> treatmentController.postTreatment(1L, 1L, treatmentDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void updateTreatmentTest() throws Exception {
        when(treatmentService.putTreatment(any(TreatmentDto.class), anyLong(), any(SecuredInfo.class), anyBoolean())).thenReturn(treatmentDto);

        ResponseEntity<Void> ret = treatmentController.updateTreatment(1L, 1L, 1L, treatmentDto, true, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(treatmentDto, ret.getBody());
    }

    @Test
    void updateTreatmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<Void> ret = treatmentController.updateTreatment(1L, 1L, 1L, treatmentDto, true, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void updateTreatmentNotMatchingTest() throws Exception {
        doThrow(new TreatmentNotMatchingException("treatment not matching")).when(apiJwtService).treatmentAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong());

        assertThrows(TreatmentNotMatchingException.class, () -> treatmentController.updateTreatment(1L, 1L, 1L, treatmentDto, true, httpServletRequest));
    }

    @Test
    void deleteTreatmentTest() throws Exception {
        ResponseEntity<Void> ret = treatmentController.deleteTreatment(1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void deleteTreatmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<Void> ret = treatmentController.deleteTreatment(1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void deleteTreatmentNotFoundTest() throws Exception {
        doThrow(new EmptyResultDataAccessException(1)).when(treatmentService).deleteById(anyLong());

        ResponseEntity<Void> ret = treatmentController.deleteTreatment(1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, ret.getStatusCode());
    }

    @Test
    void deleteTreatmentExperimentLockedTest() throws Exception {
        doThrow(new ExperimentLockedException("experiment locked")).when(apiJwtService).experimentLocked(anyLong(), anyBoolean());

        assertThrows(ExperimentLockedException.class, () -> treatmentController.deleteTreatment(1L, 1L, 1L, httpServletRequest));
    }

    @Test
    void duplicateTreatmentTest() throws Exception {
        when(assignmentTreatmentService.duplicateTreatment(anyLong(), any(SecuredInfo.class))).thenReturn(treatmentDto);
        when(treatmentService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), anyLong())).thenReturn(new HttpHeaders());

        ResponseEntity<TreatmentDto> ret = treatmentController.duplicateTreatment(1L, 1L, 1L, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(treatmentDto, ret.getBody());
    }

    @Test
    void duplicateTreatmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<TreatmentDto> ret = treatmentController.duplicateTreatment(1L, 1L, 1L, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void duplicateTreatmentExperimentLockedTest() throws Exception {
        doThrow(new ExperimentLockedException("experiment locked")).when(apiJwtService).experimentLocked(anyLong(), anyBoolean());

        assertThrows(ExperimentLockedException.class, () -> treatmentController.duplicateTreatment(1L, 1L, 1L, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

}
