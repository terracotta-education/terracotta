package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.model.dto.ExposureDto;
import edu.iu.terracotta.dao.model.enums.ExposureTypes;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentStartedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.utils.TextConstants;

public class ExposureServiceImplTest extends BaseTest {

    @InjectMocks private ExposureServiceImpl exposureService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(exposure.getExperiment()).thenReturn(experiment);
    }

    @Test
    public void testGetExposures() {
        List<ExposureDto> retVal = exposureService.getExposures(1L);

        assertEquals(1, retVal.size());
        assertEquals(EXPOSURE_TITLE, retVal.get(0).getTitle());
        assertEquals(1, retVal.get(0).getGroupConditionList().size());
    }

    @Test
    public void testGetExposuresEmptyWhenNoneFound() {
        when(exposureRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(null);

        List<ExposureDto> retVal = exposureService.getExposures(1L);

        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testPostExposureSuccess() throws Exception {
        // exposure.getUuid() isn't globally stubbed in BaseModelTest (unlike experiment.getUuid()), so stub it locally.
        UUID exposureUuid = UUID.randomUUID();
        when(exposure.getUuid()).thenReturn(exposureUuid);
        ExposureDto exposureDto = ExposureDto.builder().title("New Exposure").build();
        when(exposureRepository.save(any(Exposure.class))).thenReturn(exposure);
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.of(experiment));
        when(experimentRepository.findByUuid(experiment.getUuid())).thenReturn(experiment);

        ExposureDto retVal = exposureService.postExposure(exposureDto, 1L);

        assertNotNull(retVal);
        assertEquals(exposureUuid, retVal.getExposureId());
    }

    @Test
    public void testPostExposureIdInPostExceptionThrows() {
        ExposureDto exposureDto = ExposureDto.builder().exposureId(UUID.randomUUID()).build();

        Exception exception = assertThrows(IdInPostException.class, () -> exposureService.postExposure(exposureDto, 1L));

        assertEquals(TextConstants.ID_IN_POST_ERROR, exception.getMessage());
    }

    @Test
    public void testPostExposureTitleTooLongThrows() {
        ExposureDto exposureDto = ExposureDto.builder().title("a".repeat(256)).build();

        assertThrows(TitleValidationException.class, () -> exposureService.postExposure(exposureDto, 1L));
    }

    @Test
    public void testPostExposureExperimentNotFoundThrows() {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());
        ExposureDto exposureDto = ExposureDto.builder().title("New Exposure").build();

        Exception exception = assertThrows(DataServiceException.class, () -> exposureService.postExposure(exposureDto, 1L));

        assertEquals("Error 105: Unable to create exposure:The experiment for the exposure does not exist", exception.getMessage());
    }

    @Test
    public void testToDto() {
        // exposure.getUuid() and group.getUuid() aren't globally stubbed in BaseModelTest (unlike
        // experiment.getUuid()/condition.getUuid()), so stub them locally.
        UUID exposureUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        when(exposure.getUuid()).thenReturn(exposureUuid);
        when(group.getUuid()).thenReturn(groupUuid);

        ExposureDto retVal = exposureService.toDto(exposure);

        assertEquals(exposureUuid, retVal.getExposureId());
        assertEquals(experiment.getUuid(), retVal.getExperimentId());
        assertEquals(EXPOSURE_TITLE, retVal.getTitle());
        assertEquals(1, retVal.getGroupConditionList().size());
        assertEquals(condition.getUuid(), retVal.getGroupConditionList().get(0).getConditionId());
        assertEquals(groupUuid, retVal.getGroupConditionList().get(0).getGroupId());
    }

    @Test
    public void testFromDtoSuccess() throws DataServiceException {
        // exposureId is never populated on the incoming DTO in practice (postExposure rejects a
        // non-null one via IdInPostException before fromDto is ever reached), so fromDto no longer
        // copies it onto the entity - the DB assigns it on save.
        ExposureDto exposureDto = ExposureDto.builder().experimentId(experiment.getUuid()).title("Exposure A").build();
        when(experimentRepository.findByUuid(experiment.getUuid())).thenReturn(experiment);

        Exposure retVal = exposureService.fromDto(exposureDto);

        assertNull(retVal.getExposureId());
        assertEquals("Exposure A", retVal.getTitle());
        assertEquals(experiment, retVal.getExperiment());
    }

    @Test
    public void testFromDtoExperimentNotFoundThrows() {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());
        ExposureDto exposureDto = ExposureDto.builder().build();

        Exception exception = assertThrows(DataServiceException.class, () -> exposureService.fromDto(exposureDto));

        assertEquals("The experiment for the exposure does not exist", exception.getMessage());
    }

    @Test
    public void testCreateExposuresExperimentNotFoundThrows() {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> exposureService.createExposures(1L));

        assertEquals("The experiment for the exposure does not exist", exception.getMessage());
    }

    @Test
    public void testCreateExposuresAlreadyCorrectCountReturnsWithoutChanges() throws Exception {
        // BETWEEN experiment needs exactly 1 exposure; default experiment mock already has 1
        exposureService.createExposures(1L);

        verify(exposureRepository, never()).save(any(Exposure.class));
        verify(exposureGroupConditionRepository, never()).deleteByExposure_Experiment_ExperimentId(anyLong());
    }

    @Test
    public void testCreateExposuresMismatchedWhileStartedThrows() {
        when(experiment.getExposureType()).thenReturn(ExposureTypes.WITHIN);
        when(experiment.getConditions()).thenReturn(Arrays.asList(condition, condition));
        when(experiment.isStarted()).thenReturn(true);

        Exception exception = assertThrows(ExperimentStartedException.class, () -> exposureService.createExposures(1L));

        assertEquals("Error 110: The experiment has already started. We can't modify it", exception.getMessage());
        verify(exposureGroupConditionRepository, never()).deleteByExposure_Experiment_ExperimentId(anyLong());
    }

    @Test
    public void testCreateExposuresRecreatesWhenMismatchedAndNotStarted() throws Exception {
        when(experiment.getExposureType()).thenReturn(ExposureTypes.WITHIN);
        when(experiment.getConditions()).thenReturn(Arrays.asList(condition, condition));
        when(experiment.isStarted()).thenReturn(false);
        when(exposureRepository.save(any(Exposure.class))).thenReturn(exposure);

        exposureService.createExposures(1L);

        verify(exposureGroupConditionRepository).deleteByExposure_Experiment_ExperimentId(1L);
        verify(exposureRepository).deleteByExperiment_ExperimentId(1L);
        verify(exposureRepository, times(2)).save(any(Exposure.class));
    }

    @Test
    public void testCreateExposuresCreatesWhenNoneExist() throws Exception {
        when(experiment.getExposures()).thenReturn(Collections.emptyList());
        when(exposureRepository.save(any(Exposure.class))).thenReturn(exposure);

        exposureService.createExposures(1L);

        verify(exposureRepository, times(1)).save(any(Exposure.class));
        verify(exposureGroupConditionRepository, never()).deleteByExposure_Experiment_ExperimentId(anyLong());
    }

    @Test
    public void testGetExposure() {
        Exposure retVal = exposureService.getExposure(1L);

        assertEquals(exposure, retVal);
    }

    @Test
    public void testGetExposureByUuidFound() throws Exception {
        // exposure.getUuid() isn't globally stubbed in BaseModelTest (unlike experiment.getUuid()), so stub it locally.
        UUID uuid = UUID.randomUUID();
        when(exposure.getUuid()).thenReturn(uuid);
        when(exposureRepository.findByUuid(uuid)).thenReturn(exposure);

        Exposure retVal = exposureService.getExposureByUuid(uuid);

        assertEquals(exposure, retVal);
    }

    @Test
    public void testGetExposureByUuidNotFoundThrows() {
        UUID uuid = UUID.randomUUID();
        when(exposureRepository.findByUuid(uuid)).thenReturn(null);

        Exception exception = assertThrows(ExposureNotMatchingException.class, () -> exposureService.getExposureByUuid(uuid));

        assertTrue(exception.getMessage().startsWith("Error 108"));
    }

    @Test
    public void testUpdateExposureSuccess() throws TitleValidationException {
        ExposureDto exposureDto = ExposureDto.builder().title("New Title").build();

        exposureService.updateExposure(1L, exposureDto);

        verify(exposure).setTitle("New Title");
        verify(exposureRepository).saveAndFlush(exposure);
    }

    @Test
    public void testUpdateExposureBlankTitleThrows() {
        when(exposure.getTitle()).thenReturn("");
        ExposureDto exposureDto = ExposureDto.builder().title("").build();

        Exception exception = assertThrows(TitleValidationException.class, () -> exposureService.updateExposure(1L, exposureDto));

        assertEquals("Error 100: Please give the exposure a title.", exception.getMessage());
    }

    @Test
    public void testUpdateExposureTitleTooLongThrows() {
        ExposureDto exposureDto = ExposureDto.builder().title("a".repeat(256)).build();

        Exception exception = assertThrows(TitleValidationException.class, () -> exposureService.updateExposure(1L, exposureDto));

        assertEquals("Error 101: Title must be 255 characters or less.", exception.getMessage());
    }

    @Test
    public void testDeleteById() {
        exposureService.deleteById(1L);

        verify(exposureRepository).deleteByExposureId(1L);
    }

    @Test
    public void testValidateTitleValid() {
        assertDoesNotThrow(() -> exposureService.validateTitle("Valid Title"));
    }

    @Test
    public void testValidateTitleTooLongThrows() {
        Exception exception = assertThrows(TitleValidationException.class, () -> exposureService.validateTitle("a".repeat(256)));

        assertEquals("Title must be 255 characters or less.", exception.getMessage());
    }

    @Test
    public void testBuildHeaders() {
        UUID experimentUuid = UUID.randomUUID();
        UUID exposureUuid = UUID.randomUUID();

        HttpHeaders retVal = exposureService.buildHeaders(UriComponentsBuilder.newInstance(), experimentUuid, exposureUuid);

        assertNotNull(retVal);
        assertTrue(retVal.getLocation().toString().contains("/api/experiments/" + experimentUuid + "/exposures/" + exposureUuid));
    }

}
