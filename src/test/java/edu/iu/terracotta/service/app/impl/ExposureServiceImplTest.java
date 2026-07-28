package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Exposure;
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
        ExposureDto exposureDto = ExposureDto.builder().title("New Exposure").build();
        when(exposureRepository.save(any(Exposure.class))).thenReturn(exposure);

        ExposureDto retVal = exposureService.postExposure(exposureDto, 1L);

        assertNotNull(retVal);
        assertEquals(1L, retVal.getExposureId());
    }

    @Test
    public void testPostExposureIdInPostExceptionThrows() {
        ExposureDto exposureDto = ExposureDto.builder().exposureId(5L).build();

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
        ExposureDto retVal = exposureService.toDto(exposure);

        assertEquals(1L, retVal.getExposureId());
        assertEquals(1L, retVal.getExperimentId());
        assertEquals(EXPOSURE_TITLE, retVal.getTitle());
        assertEquals(1, retVal.getGroupConditionList().size());
        assertEquals(1L, retVal.getGroupConditionList().get(0).getConditionId());
        assertEquals(1L, retVal.getGroupConditionList().get(0).getGroupId());
    }

    @Test
    public void testFromDtoSuccess() throws DataServiceException {
        ExposureDto exposureDto = ExposureDto.builder().exposureId(1L).experimentId(1L).title("Exposure A").build();

        Exposure retVal = exposureService.fromDto(exposureDto);

        assertEquals(1L, retVal.getExposureId());
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
        HttpHeaders retVal = exposureService.buildHeaders(UriComponentsBuilder.newInstance(), 1L, 2L);

        assertNotNull(retVal);
        assertTrue(retVal.getLocation().toString().contains("/api/experiments/1/exposures/2"));
    }

}
