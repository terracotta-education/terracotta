package edu.iu.terracotta.service.app;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.model.dto.AssignmentFileArchiveDto;
import edu.iu.terracotta.exceptions.AssignmentFileArchiveNotFoundException;

public interface AssignmentFileArchiveService {

    AssignmentFileArchiveDto process(Assignment assignment, SecuredInfo securedInfo) throws IOException;
    AssignmentFileArchiveDto poll(Assignment assignment, SecuredInfo securedInfo, boolean createNewOnOutdated) throws IOException, AssignmentFileArchiveNotFoundException;
    AssignmentFileArchiveDto retrieve(UUID uuid, Assignment assignment, SecuredInfo securedInfo) throws IOException;
    Optional<AssignmentFileArchive> findLatestAvailableArchive(long assignmentId) throws IOException;
    void errorAcknowledge(UUID uuid, Assignment assignment) throws IOException, AssignmentFileArchiveNotFoundException;
    AssignmentFileArchiveDto toDto(AssignmentFileArchive assignmentFileArchive, boolean includeFileContent) throws IOException;

}
