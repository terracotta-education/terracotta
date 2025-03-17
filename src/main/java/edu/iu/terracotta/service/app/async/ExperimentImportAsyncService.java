package edu.iu.terracotta.service.app.async;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.exceptions.ExperimentImportException;

public interface ExperimentImportAsyncService {

    void process(ExperimentImport experimentImport, SecuredInfo securedInfo) throws ExperimentImportException;

}
