package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.dto.ExposureDto;
import edu.iu.terracotta.repository.AllRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ExposureService {

    @Autowired
    AllRepositories allRepositories;

    public List<Exposure> findAllByExperimentId(long experimentId) {
        return allRepositories.exposureRepository.findByExperiment_ExperimentId(experimentId);
    }

    public Optional<Exposure> findOneByExposureId(long exposureId) {
        return allRepositories.exposureRepository.findByExposureId(exposureId);
    }

    public ExposureDto toDto(Exposure exposure) {

        ExposureDto exposureDto = new ExposureDto();
        exposureDto.setExposureId(exposure.getExposureId());
        //TODO check null?
        exposureDto.setExperimentId(exposure.getExperiment().getExperimentId());
        exposureDto.setTitle(exposure.getTitle());

        return exposureDto;
    }

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

    public Exposure save (Exposure exposure) { return allRepositories.exposureRepository.save(exposure); }

    public Optional<Exposure> findById(Long id) { return allRepositories.exposureRepository.findById(id); }

    public void saveAndFlush(Exposure exposureToChange) { allRepositories.exposureRepository.saveAndFlush(exposureToChange); }

    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.exposureRepository.deleteById(id); }

}
