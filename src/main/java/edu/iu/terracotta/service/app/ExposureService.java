package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentStartedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.dto.ExposureDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

public interface ExposureService {

    List<ExposureDto> getExposures(Long experimentId);

    ExposureDto postExposure(ExposureDto exposureDto, long experimentId) throws IdInPostException, DataServiceException, TitleValidationException;

    ExposureDto toDto(Exposure exposure);

    Exposure fromDto(ExposureDto exposureDto) throws DataServiceException;

    Exposure getExposure(Long id);

    void updateExposure(Long exposureId, ExposureDto exposureDto)throws TitleValidationException;

    void deleteById(Long id) throws EmptyResultDataAccessException;

    void createExposures(Long experimentId) throws DataServiceException, ExperimentStartedException;

    void validateTitle(String title) throws TitleValidationException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long exposureId);
}
