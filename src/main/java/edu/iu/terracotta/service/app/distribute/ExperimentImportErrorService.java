package edu.iu.terracotta.service.app.distribute;

import java.util.List;

import edu.iu.terracotta.dao.entity.distribute.ExperimentImportError;
import edu.iu.terracotta.dao.model.dto.distribute.ExperimentImportErrorDto;

public interface ExperimentImportErrorService {

    List<ExperimentImportErrorDto> toDto(List<ExperimentImportError> experimentImportErrors);
    ExperimentImportErrorDto toDto(ExperimentImportError experimentImportError);

}
