package edu.iu.terracotta.service.app.distribute;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.model.dto.distribute.ImportDto;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.exceptions.ExperimentImportException;

public interface ExperimentImportService {

    ImportDto preprocess(MultipartFile file, SecuredInfo securedInfo) throws ExperimentImportException;

    /**
     * Same as preprocess(MultipartFile, SecuredInfo), for a File that already exists on disk
     * instead of an uploaded MultipartFile - used to feed an in-process export (see
     * ExperimentCopyCandidateServiceImpl) into the same import pipeline a manual zip upload uses,
     * without fabricating a fake MultipartFile (its test double isn't available to main code).
     * assignmentRepointMap: see ExperimentImportAsyncService.process - empty for a normal import.
     */
    ImportDto preprocessFromFile(File file, String originalFilename, SecuredInfo securedInfo, Map<Long, LmsAssignment> assignmentRepointMap) throws ExperimentImportException;
    ImportDto preprocessError(MultipartFile file, String errorMessage, SecuredInfo securedInfo);
    ExperimentImport validate(ExperimentImport experimentImport);
    ImportDto acknowledge(ExperimentImport experimentImport, ExperimentImportStatus experimentImportStatus);
    List<ImportDto> getAll(SecuredInfo securedInfo);
    ImportDto toDto(ExperimentImport experimentImport);
    List<ImportDto> toDto(List<ExperimentImport> experimentImports);

}
