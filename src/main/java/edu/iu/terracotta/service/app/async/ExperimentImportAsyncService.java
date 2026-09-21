package edu.iu.terracotta.service.app.async;

import java.util.Map;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.exceptions.ExperimentImportException;

public interface ExperimentImportAsyncService {

    /**
     * assignmentRepointMap: source (old) Assignment ID -> an already-existing LMS assignment to
     * re-point at the newly-created Assignment for that source ID, instead of creating a new one.
     * Empty for a normal (manual zip upload) import.
     */
    void process(ExperimentImport experimentImport, SecuredInfo securedInfo, Map<Long, LmsAssignment> assignmentRepointMap) throws ExperimentImportException;

}
