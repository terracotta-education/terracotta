package edu.iu.terracotta.service.app.distribute;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.model.dto.distribute.ImportDto;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.exceptions.ExperimentImportException;

public interface ExperimentImportService {

    ImportDto preprocess(MultipartFile file, SecuredInfo securedInfo) throws ExperimentImportException;
    ImportDto preprocessError(MultipartFile file, String errorMessage, SecuredInfo securedInfo);
    void validate(ExperimentImport experimentImport);
    ImportDto acknowledge(ExperimentImport experimentImport, ExperimentImportStatus experimentImportStatus);
    List<ImportDto> getAll(SecuredInfo securedInfo);
    ImportDto toDto(ExperimentImport experimentImport);
    List<ImportDto> toDto(List<ExperimentImport> experimentImports);

}
