package edu.iu.terracotta.service.app.distribute.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.distribute.ExperimentImportError;
import edu.iu.terracotta.dao.model.dto.distribute.ExperimentImportErrorDto;
import edu.iu.terracotta.service.app.distribute.ExperimentImportErrorService;

@Service
public class ExperimentImportErrorServiceImpl implements ExperimentImportErrorService {

    @Override
    public List<ExperimentImportErrorDto> toDto(List<ExperimentImportError> experimentImportErrors) {
        return experimentImportErrors.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ExperimentImportErrorDto toDto(ExperimentImportError experimentImportError) {
        return ExperimentImportErrorDto.builder()
            .experimentImportId(experimentImportError.getExperimentImport().getUuid())
            .id(experimentImportError.getUuid())
            .text(experimentImportError.getText())
            .build();
    }

}
