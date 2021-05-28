package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.dto.ExposureDto;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface ExposureService {
    List<Exposure> findAllByExperimentId(long experimentId);

    ExposureDto toDto(Exposure exposure);

    Exposure fromDto(ExposureDto exposureDto) throws DataServiceException;

    Exposure save(Exposure exposure);

    Optional<Exposure> findById(Long id);

    void saveAndFlush(Exposure exposureToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean exposureBelongsToExperiment(Long experimentId, Long exposureId);
}
