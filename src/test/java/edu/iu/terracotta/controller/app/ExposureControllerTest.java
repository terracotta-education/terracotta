package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.model.dto.ExposureDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentStartedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;

public class ExposureControllerTest extends BaseTest {

    private ExposureController exposureController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // ApiJwtService has two matching mocks in BaseServiceTest (apiJwtService and canvasApiJwtService),
        // so the controller is constructed manually rather than relying on @InjectMocks to avoid ambiguous wiring.
        exposureController = new ExposureController(exposureService, apiJwtService);

        when(apiJwtService.extractValues(any(), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.experimentAllowed(any(), anyLong())).thenReturn(experiment);
        when(apiJwtService.exposureAllowed(any(), anyLong(), anyLong())).thenReturn(exposure);
        when(apiJwtService.experimentLocked(anyLong(), anyBoolean())).thenReturn(false);
    }

    @Test
    void allExposuresByExperimentExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> exposureController.allExposuresByExperiment(1L, httpServletRequest));
    }

    @Test
    void allExposuresByExperimentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<ExposureDto>> ret = exposureController.allExposuresByExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void allExposuresByExperimentNoContentTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(exposureService.getExposures(1L)).thenReturn(List.of());

        ResponseEntity<List<ExposureDto>> ret = exposureController.allExposuresByExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, ret.getStatusCode());
    }

    @Test
    void allExposuresByExperimentSuccessTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        ExposureDto exposureDto = ExposureDto.builder().exposureId(1L).title("exposure").build();
        when(exposureService.getExposures(1L)).thenReturn(List.of(exposureDto));

        ResponseEntity<List<ExposureDto>> ret = exposureController.allExposuresByExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(List.of(exposureDto), ret.getBody());
    }

    @Test
    void getExposureExposureNotMatchingTest() throws Exception {
        doThrow(new ExposureNotMatchingException("not matching")).when(apiJwtService).exposureAllowed(securedInfo, 1L, 1L);

        assertThrows(ExposureNotMatchingException.class, () -> exposureController.getExposure(1L, 1L, httpServletRequest));
    }

    @Test
    void getExposureUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ExposureDto> ret = exposureController.getExposure(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void getExposureSuccessTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        ExposureDto exposureDto = ExposureDto.builder().exposureId(1L).title("exposure").build();
        when(exposureService.getExposure(1L)).thenReturn(exposure);
        when(exposureService.toDto(exposure)).thenReturn(exposureDto);

        ResponseEntity<ExposureDto> ret = exposureController.getExposure(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(exposureDto, ret.getBody());
    }

    @Test
    void postExposureExperimentLockedTest() throws Exception {
        doThrow(new ExperimentLockedException("locked")).when(apiJwtService).experimentLocked(1L, true);

        ExposureDto exposureDto = ExposureDto.builder().title("exposure").build();

        assertThrows(
            ExperimentLockedException.class,
            () -> exposureController.postExposure(1L, exposureDto, UriComponentsBuilder.newInstance(), httpServletRequest)
        );
    }

    @Test
    void postExposureUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        ExposureDto exposureDto = ExposureDto.builder().title("exposure").build();

        ResponseEntity<ExposureDto> ret = exposureController.postExposure(1L, exposureDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void postExposureSuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExposureDto exposureDto = ExposureDto.builder().title("exposure").build();
        ExposureDto returnedDto = ExposureDto.builder().exposureId(1L).title("exposure").build();
        when(exposureService.postExposure(exposureDto, 1L)).thenReturn(returnedDto);

        ResponseEntity<ExposureDto> ret = exposureController.postExposure(1L, exposureDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(returnedDto, ret.getBody());
    }

    @Test
    void postExposureTitleValidationTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExposureDto exposureDto = ExposureDto.builder().title("").build();
        doThrow(new TitleValidationException("invalid title")).when(exposureService).postExposure(exposureDto, 1L);

        assertThrows(
            TitleValidationException.class,
            () -> exposureController.postExposure(1L, exposureDto, UriComponentsBuilder.newInstance(), httpServletRequest)
        );
    }

    @Test
    void postExposureIdInPostTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExposureDto exposureDto = ExposureDto.builder().exposureId(1L).title("exposure").build();
        doThrow(new IdInPostException("id in post")).when(exposureService).postExposure(exposureDto, 1L);

        assertThrows(
            IdInPostException.class,
            () -> exposureController.postExposure(1L, exposureDto, UriComponentsBuilder.newInstance(), httpServletRequest)
        );
    }

    @Test
    void postExposureDataServiceExceptionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExposureDto exposureDto = ExposureDto.builder().title("exposure").build();
        doThrow(new DataServiceException("failed")).when(exposureService).postExposure(exposureDto, 1L);

        assertThrows(
            DataServiceException.class,
            () -> exposureController.postExposure(1L, exposureDto, UriComponentsBuilder.newInstance(), httpServletRequest)
        );
    }

    @Test
    void createExposuresExperimentLockedTest() throws Exception {
        doThrow(new ExperimentLockedException("locked")).when(apiJwtService).experimentLocked(1L, true);

        assertThrows(ExperimentLockedException.class, () -> exposureController.createExposures(1L, httpServletRequest));
    }

    @Test
    void createExposuresUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> ret = exposureController.createExposures(1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void createExposuresSuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Void> ret = exposureController.createExposures(1L, httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
    }

    @Test
    void createExposuresExperimentStartedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new ExperimentStartedException("already started")).when(exposureService).createExposures(1L);

        assertThrows(ExperimentStartedException.class, () -> exposureController.createExposures(1L, httpServletRequest));
    }

    @Test
    void createExposuresDataServiceExceptionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new DataServiceException("failed")).when(exposureService).createExposures(1L);

        assertThrows(DataServiceException.class, () -> exposureController.createExposures(1L, httpServletRequest));
    }

    @Test
    void updateExposureExposureNotMatchingTest() throws Exception {
        doThrow(new ExposureNotMatchingException("not matching")).when(apiJwtService).exposureAllowed(securedInfo, 1L, 1L);
        ExposureDto exposureDto = ExposureDto.builder().title("exposure").build();

        assertThrows(ExposureNotMatchingException.class, () -> exposureController.updateExposure(1L, 1L, exposureDto, httpServletRequest));
    }

    @Test
    void updateExposureUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        ExposureDto exposureDto = ExposureDto.builder().title("exposure").build();

        ResponseEntity<Void> ret = exposureController.updateExposure(1L, 1L, exposureDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void updateExposureSuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExposureDto exposureDto = ExposureDto.builder().title("exposure").build();

        ResponseEntity<Void> ret = exposureController.updateExposure(1L, 1L, exposureDto, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void updateExposureTitleValidationTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExposureDto exposureDto = ExposureDto.builder().title("").build();
        doThrow(new TitleValidationException("invalid title")).when(exposureService).updateExposure(1L, exposureDto);

        assertThrows(TitleValidationException.class, () -> exposureController.updateExposure(1L, 1L, exposureDto, httpServletRequest));
    }

    @Test
    void deleteExposureExperimentLockedTest() throws Exception {
        doThrow(new ExperimentLockedException("locked")).when(apiJwtService).experimentLocked(1L, true);

        assertThrows(ExperimentLockedException.class, () -> exposureController.deleteExposure(1L, 1L, httpServletRequest));
    }

    @Test
    void deleteExposureUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> ret = exposureController.deleteExposure(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void deleteExposureSuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Void> ret = exposureController.deleteExposure(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void deleteExposureNotFoundTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new EmptyResultDataAccessException(1)).when(exposureService).deleteById(1L);

        ResponseEntity<Void> ret = exposureController.deleteExposure(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, ret.getStatusCode());
    }

}
