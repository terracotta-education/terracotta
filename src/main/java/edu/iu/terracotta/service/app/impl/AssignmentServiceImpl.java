package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Exposure;

import java.util.List;
import java.util.Optional;

@Component
public class AssignmentServiceImpl implements AssignmentService {

    @Autowired
    AllRepositories allRepositories;

    @Override
    public List<Assignment> findAllByExposureId(long exposureId) {
        return allRepositories.assignmentRepository.findByExposure_ExposureId(exposureId);
    }

    @Override
    public AssignmentDto toDto(Assignment assignment) {

        AssignmentDto assignmentDto = new AssignmentDto();
        assignmentDto.setAssignmentId(assignment.getAssignmentId());
        assignmentDto.setLmsAssignmentId(assignment.getLmsAssignmentId());
        assignmentDto.setTitle(assignment.getTitle());
        assignmentDto.setAssignmentOrder(assignment.getAssignmentOrder());
        assignmentDto.setExposureId(assignment.getExposure().getExposureId());
        assignmentDto.setResourceLinkId(assignment.getResourceLinkId());

        return assignmentDto;
    }

    @Override
    public Assignment fromDto(AssignmentDto assignmentDto) throws DataServiceException {

        Assignment assignment = new Assignment();
        assignment.setAssignmentId(assignmentDto.getAssignmentId());
        //We don't need these in the object.
        //assignment.setLmsAssignmentId(assignmentDto.getLmsAssignmentId());
        //assignment.setResourceLinkId(assignmentDto.getResourceLinkId());
        assignment.setTitle(assignmentDto.getTitle());
        assignment.setAssignmentOrder(assignmentDto.getAssignmentOrder());
        Optional<Exposure> exposure = allRepositories.exposureRepository.findById(assignmentDto.getExposureId());
        if(exposure.isPresent()) {
            assignment.setExposure(exposure.get());
        } else {
            throw new DataServiceException("The exposure for the assignment does not exist");
        }
        return assignment;
    }

    @Override
    public Assignment save(Assignment assignment) { return allRepositories.assignmentRepository.save(assignment); }

    @Override
    public Optional<Assignment> findById(Long id) { return allRepositories.assignmentRepository.findById(id); }

    @Override
    public void saveAndFlush(Assignment assignmentToChange) { allRepositories.assignmentRepository.saveAndFlush(assignmentToChange); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.assignmentRepository.deleteById(id); }

    @Override
    public boolean assignmentBelongsToExperimentAndExposure(Long experimentId, Long exposureId, Long assignmentId) {
        return allRepositories.assignmentRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(experimentId, exposureId, assignmentId);
    }

    @Override
    public boolean assignmentBelongsToExperiment(Long experimentId, Long assignmentId) {
        return allRepositories.assignmentRepository.existsByExposure_Experiment_ExperimentIdAndAssignmentId(experimentId,assignmentId); }
}
