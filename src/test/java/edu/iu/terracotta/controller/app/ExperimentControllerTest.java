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
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.ExperimentDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.WrongValueException;

public class ExperimentControllerTest extends BaseTest {

    private ExperimentController experimentController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually (not @InjectMocks) because ApiJwtService has two type-matching
        // mock candidates in BaseServiceTest (apiJwtService and canvasApiJwtService), and
        // Mockito's constructor injection matches by type only, with no field-name tiebreak.
        experimentController = new ExperimentController(experimentService, apiJwtService);

        when(apiJwtService.extractValues(any(), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.experimentAllowed(any(), anyLong())).thenReturn(experiment);
        when(apiJwtService.experimentLocked(anyLong(), anyBoolean())).thenReturn(false);
    }

    @Test
    void allExperimentsByCourseBadTokenTest() throws Exception {
        when(apiJwtService.extractValues(any(), anyBoolean())).thenReturn(null);

        assertThrows(BadTokenException.class, () -> experimentController.allExperimentsByCourse(httpServletRequest));
    }

    @Test
    void allExperimentsByCourseUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<ExperimentDto>> ret = experimentController.allExperimentsByCourse(httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void allExperimentsByCourseNoContentTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(experimentService.getExperiments(securedInfo, true)).thenReturn(List.of());

        ResponseEntity<List<ExperimentDto>> ret = experimentController.allExperimentsByCourse(httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, ret.getStatusCode());
    }

    @Test
    void allExperimentsByCourseSuccessTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().experimentId(1L).title("experiment").build();
        when(experimentService.getExperiments(securedInfo, true)).thenReturn(List.of(experimentDto));

        ResponseEntity<List<ExperimentDto>> ret = experimentController.allExperimentsByCourse(httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(List.of(experimentDto), ret.getBody());
    }

    @Test
    void getExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> experimentController.getExperiment(1L, false, false, false, httpServletRequest));
    }

    @Test
    void getExperimentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ExperimentDto> ret = experimentController.getExperiment(1L, false, false, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void getExperimentSuccessTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().experimentId(1L).title("experiment").build();
        when(experimentService.getExperiment(1L)).thenReturn(experiment);
        when(experimentService.toDto(experiment, true, true, true, securedInfo)).thenReturn(experimentDto);

        ResponseEntity<ExperimentDto> ret = experimentController.getExperiment(1L, true, true, true, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(experimentDto, ret.getBody());
    }

    @Test
    void postExperimentBadTokenTest() throws Exception {
        when(apiJwtService.extractValues(any(), anyBoolean())).thenReturn(null);
        ExperimentDto experimentDto = ExperimentDto.builder().title("new experiment").build();

        assertThrows(
            BadTokenException.class,
            () -> experimentController.postExperiment(experimentDto, UriComponentsBuilder.newInstance(), httpServletRequest)
        );
    }

    @Test
    void postExperimentNullDtoBadTokenTest() throws Exception {
        when(apiJwtService.extractValues(any(), anyBoolean())).thenReturn(null);

        assertThrows(
            BadTokenException.class,
            () -> experimentController.postExperiment(null, UriComponentsBuilder.newInstance(), httpServletRequest)
        );
    }

    @Test
    void postExperimentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        ExperimentDto experimentDto = ExperimentDto.builder().title("new experiment").build();

        ResponseEntity<ExperimentDto> ret = experimentController.postExperiment(experimentDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void postExperimentIdInPostTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().experimentId(1L).title("new experiment").build();

        assertThrows(
            IdInPostException.class,
            () -> experimentController.postExperiment(experimentDto, UriComponentsBuilder.newInstance(), httpServletRequest)
        );
    }

    @Test
    void postExperimentExistingEmptyTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().title("new experiment").build();
        ExperimentDto existingEmpty = ExperimentDto.builder().experimentId(2L).title(null).build();
        when(experimentService.getEmptyExperiment(securedInfo, experimentDto)).thenReturn(existingEmpty);

        ResponseEntity<ExperimentDto> ret = experimentController.postExperiment(experimentDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.ALREADY_REPORTED, ret.getStatusCode());
        assertEquals(existingEmpty, ret.getBody());
    }

    @Test
    void postExperimentSuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().title("new experiment").build();
        ExperimentDto returnedDto = ExperimentDto.builder().experimentId(3L).title("new experiment").build();
        when(experimentService.getEmptyExperiment(securedInfo, experimentDto)).thenReturn(null);
        when(experimentService.postExperiment(experimentDto, securedInfo)).thenReturn(returnedDto);

        ResponseEntity<ExperimentDto> ret = experimentController.postExperiment(experimentDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(returnedDto, ret.getBody());
    }

    @Test
    void postExperimentNullBodySuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExperimentDto returnedDto = ExperimentDto.builder().experimentId(4L).build();
        when(experimentService.getEmptyExperiment(any(SecuredInfo.class), any(ExperimentDto.class))).thenReturn(null);
        when(experimentService.postExperiment(any(ExperimentDto.class), any())).thenReturn(returnedDto);

        ResponseEntity<ExperimentDto> ret = experimentController.postExperiment(null, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(returnedDto, ret.getBody());
    }

    @Test
    void postExperimentTitleValidationTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().title("").build();
        when(experimentService.getEmptyExperiment(securedInfo, experimentDto)).thenReturn(null);
        doThrow(new TitleValidationException("invalid title")).when(experimentService).postExperiment(experimentDto, securedInfo);

        assertThrows(
            TitleValidationException.class,
            () -> experimentController.postExperiment(experimentDto, UriComponentsBuilder.newInstance(), httpServletRequest)
        );
    }

    @Test
    void postExperimentDataServiceExceptionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().title("new experiment").build();
        when(experimentService.getEmptyExperiment(securedInfo, experimentDto)).thenReturn(null);
        doThrow(new DataServiceException("failed")).when(experimentService).postExperiment(experimentDto, securedInfo);

        assertThrows(
            DataServiceException.class,
            () -> experimentController.postExperiment(experimentDto, UriComponentsBuilder.newInstance(), httpServletRequest)
        );
    }

    @Test
    void updateExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(securedInfo, 1L);
        ExperimentDto experimentDto = ExperimentDto.builder().title("updated").build();

        assertThrows(ExperimentNotMatchingException.class, () -> experimentController.updateExperiment(1L, experimentDto, httpServletRequest));
    }

    @Test
    void updateExperimentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        ExperimentDto experimentDto = ExperimentDto.builder().title("updated").build();

        ResponseEntity<Void> ret = experimentController.updateExperiment(1L, experimentDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void updateExperimentSuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().title("updated").build();

        ResponseEntity<Void> ret = experimentController.updateExperiment(1L, experimentDto, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void updateExperimentWrongValueTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().title("updated").build();
        doThrow(new WrongValueException("wrong value")).when(experimentService).updateExperiment(1L, 1L, experimentDto, securedInfo);

        assertThrows(WrongValueException.class, () -> experimentController.updateExperiment(1L, experimentDto, httpServletRequest));
    }

    @Test
    void updateExperimentParticipantNotUpdatedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        ExperimentDto experimentDto = ExperimentDto.builder().title("updated").build();
        doThrow(new ParticipantNotUpdatedException("not updated")).when(experimentService).updateExperiment(1L, 1L, experimentDto, securedInfo);

        assertThrows(ParticipantNotUpdatedException.class, () -> experimentController.updateExperiment(1L, experimentDto, httpServletRequest));
    }

    @Test
    void deleteExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> experimentController.deleteExperiment(1L, httpServletRequest));
    }

    @Test
    void deleteExperimentLockedTest() throws Exception {
        doThrow(new ExperimentLockedException("locked")).when(apiJwtService).experimentLocked(1L, true);

        assertThrows(ExperimentLockedException.class, () -> experimentController.deleteExperiment(1L, httpServletRequest));
    }

    @Test
    void deleteExperimentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> ret = experimentController.deleteExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void deleteExperimentSuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Void> ret = experimentController.deleteExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void deleteExperimentEmptyResultReturnsNotFoundTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new EmptyResultDataAccessException(1)).when(experimentService).deleteById(1L, securedInfo);

        ResponseEntity<Void> ret = experimentController.deleteExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, ret.getStatusCode());
    }

}
