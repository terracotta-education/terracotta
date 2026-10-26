package edu.iu.terracotta.service.app;

import java.io.IOException;
import java.util.List;
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
    /**
     * The experiment's assignments whose latest file archive has uploads newer than it (and the
     * instructor hasn't dismissed that since), each as its latest archive.
     */
    List<AssignmentFileArchiveDto> findOutdated(long experimentId) throws IOException;
    /**
     * Dismisses the "new uploads since the last archive" indicator for an outdated archive, until
     * there are uploads newer than this.
     */
    void outdatedAcknowledge(UUID uuid, Assignment assignment) throws AssignmentFileArchiveNotFoundException;
    void errorAcknowledge(UUID uuid, Assignment assignment) throws IOException, AssignmentFileArchiveNotFoundException;
    AssignmentFileArchiveDto toDto(AssignmentFileArchive assignmentFileArchive, boolean includeFileContent) throws IOException;

}
