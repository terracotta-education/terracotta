package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.ParticipantDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ParticipantAlreadyStartedException;
import edu.iu.terracotta.utils.TextConstants;

public class ParticipantControllerTest extends BaseTest {

    private ParticipantController participantController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        participantController = new ParticipantController(participantService, apiJwtService);

        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
    }

    @Test
    void allParticipantsByExperimentHappyPathTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(participantService.getParticipants(1L, USER_ID, false, securedInfo, true)).thenReturn(List.of(participantDto));

        ResponseEntity<List<ParticipantDto>> response = participantController.allParticipantsByExperiment(1L, true, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void allParticipantsByExperimentEmptyTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(participantService.getParticipants(1L, USER_ID, true, securedInfo, false)).thenReturn(Collections.emptyList());

        ResponseEntity<List<ParticipantDto>> response = participantController.allParticipantsByExperiment(1L, false, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void allParticipantsByExperimentPermissionDeniedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<ParticipantDto>> response = participantController.allParticipantsByExperiment(1L, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void allParticipantsByExperimentPropagatesExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("no match")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> participantController.allParticipantsByExperiment(1L, false, httpServletRequest));
    }

    @Test
    void getParticipantHappyPathTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(participantService.getParticipant(2L, 1L, USER_ID, false)).thenReturn(participant);
        when(participantService.toDto(participant, securedInfo)).thenReturn(participantDto);

        ResponseEntity<ParticipantDto> response = participantController.getParticipant(1L, 2L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(participantDto, response.getBody());
    }

    @Test
    void getParticipantPermissionDeniedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ParticipantDto> response = participantController.getParticipant(1L, 2L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void postParticipantHappyPathTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(participantService.postParticipant(participantDto, 1L, securedInfo)).thenReturn(participantDto);
        when(participantService.buildHeaders(any(UriComponentsBuilder.class), eq(1L), eq(1L))).thenReturn(new HttpHeaders());

        ResponseEntity<ParticipantDto> response = participantController.postParticipant(1L, participantDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(participantDto, response.getBody());
    }

    @Test
    void postParticipantPermissionDeniedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ParticipantDto> response = participantController.postParticipant(1L, participantDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void postParticipantPropagatesTerracottaConnectorExceptionTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenThrow(new TerracottaConnectorException("boom"));

        assertThrows(TerracottaConnectorException.class, () -> participantController.postParticipant(1L, participantDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void updateParticipantInstructorHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(participantService.getParticipant(2L, 1L, USER_ID, false)).thenReturn(participant);
        when(participantService.changeParticipant(anyMap(), eq(1L), eq(securedInfo))).thenReturn(List.of(participant));
        when(participantService.toDto(participant, securedInfo)).thenReturn(participantDto);

        ResponseEntity<ParticipantDto> response = participantController.updateParticipant(1L, 2L, participantDto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(participantDto, response.getBody());
    }

    @Test
    void updateParticipantLearnerHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(participantService.getParticipant(2L, 1L, USER_ID, true)).thenReturn(participant);
        when(participantService.changeConsent(participantDto, securedInfo, 1L)).thenReturn(participant);
        when(participantService.toDto(participant, securedInfo)).thenReturn(participantDto);

        ResponseEntity<ParticipantDto> response = participantController.updateParticipant(1L, 2L, participantDto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(participantDto, response.getBody());
        verify(participantService).postConsentSubmission(participant, securedInfo);
    }

    @Test
    void updateParticipantLearnerAlreadyStartedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(participantService.getParticipant(2L, 1L, USER_ID, true)).thenReturn(participant);
        doThrow(new ParticipantAlreadyStartedException("started")).when(participantService).changeConsent(participantDto, securedInfo, 1L);

        ResponseEntity<ParticipantDto> response = participantController.updateParticipant(1L, 2L, participantDto, httpServletRequest);
        // body is actually a plain String in this branch (controller uses a raw ResponseEntity), so
        // keep the reference as Object here - calling .toString() through the ParticipantDto-typed
        // getter would insert a checkcast to ParticipantDto and throw a ClassCastException at runtime.
        Object body = response.getBody();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(body.toString().contains("Error 149"));
    }

    @Test
    void updateParticipantLearnerConnectionExceptionWrappedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(participantService.getParticipant(2L, 1L, USER_ID, true)).thenReturn(participant);
        doThrow(new ConnectionException("lms down")).when(participantService).postConsentSubmission(participant, securedInfo);

        assertThrows(RuntimeException.class, () -> participantController.updateParticipant(1L, 2L, participantDto, httpServletRequest));
    }

    @Test
    void updateParticipantPermissionDeniedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(apiJwtService.isLearner(securedInfo)).thenReturn(false);

        ResponseEntity<ParticipantDto> response = participantController.updateParticipant(1L, 2L, participantDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void updateParticipantsHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(participantService.getParticipant(1L, 1L, USER_ID, false)).thenReturn(participant);

        ResponseEntity<Void> response = participantController.updateParticipants(1L, List.of(participantDto), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void updateParticipantsPermissionDeniedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = participantController.updateParticipants(1L, List.of(participantDto), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void updateParticipantsWrapsUnexpectedExceptionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(participantService.getParticipant(1L, 1L, USER_ID, false)).thenReturn(participant);
        doThrow(new RuntimeException("db fail")).when(participantService).changeParticipant(anyMap(), eq(1L), eq(securedInfo));

        assertThrows(DataServiceException.class, () -> participantController.updateParticipants(1L, List.of(participantDto), httpServletRequest));
    }

    @Test
    void deleteParticipantHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(participantService.getParticipant(2L, 1L, USER_ID, false)).thenReturn(participant);

        ResponseEntity<Void> response = participantController.deleteParticipant(1L, 2L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(participant).setDropped(true);
        verify(participantService, times(1)).saveAndFlush(participant);
    }

    @Test
    void deleteParticipantPermissionDeniedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = participantController.deleteParticipant(1L, 2L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

}
