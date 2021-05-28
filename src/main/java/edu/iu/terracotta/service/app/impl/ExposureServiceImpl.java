package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.dto.ExposureDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExposureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ExposureServiceImpl implements ExposureService {

    @Autowired
    AllRepositories allRepositories;

    @Override
    public List<Exposure> findAllByExperimentId(long experimentId) {
        return allRepositories.exposureRepository.findByExperiment_ExperimentId(experimentId);
    }

    @Override
    public ExposureDto toDto(Exposure exposure) {

        ExposureDto exposureDto = new ExposureDto();
        exposureDto.setExposureId(exposure.getExposureId());
        //TODO check null?
        exposureDto.setExperimentId(exposure.getExperiment().getExperimentId());
        exposureDto.setTitle(exposure.getTitle());

        return exposureDto;
    }

    @Override
    public Exposure fromDto(ExposureDto exposureDto) throws DataServiceException {

        Exposure exposure = new Exposure();
        //test if nulls behave correctly?
        exposure.setExposureId(exposureDto.getExposureId());
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(exposureDto.getExperimentId());
        if(experiment.isPresent()) {
            exposure.setExperiment(experiment.get());
        } else {
            throw new DataServiceException("The experiment for the exposure does not exist");
        }

        exposure.setTitle(exposureDto.getTitle());
        return exposure;
    }

    @Override
    public Exposure save(Exposure exposure) { return allRepositories.exposureRepository.save(exposure); }

    @Override
    public Optional<Exposure> findById(Long id) { return allRepositories.exposureRepository.findById(id); }

    @Override
    public void saveAndFlush(Exposure exposureToChange) { allRepositories.exposureRepository.saveAndFlush(exposureToChange); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.exposureRepository.deleteById(id); }

    @Override
    public boolean exposureBelongsToExperiment(Long experimentId, Long exposureId) {
        return allRepositories.exposureRepository.existsByExperiment_ExperimentIdAndExposureId(experimentId,exposureId);
    }

}
