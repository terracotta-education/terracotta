package edu.iu.terracotta.service.app.async;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.model.distribute.LmsRepointTargets;
import edu.iu.terracotta.exceptions.ExperimentImportException;

public interface ExperimentImportAsyncService {

    /**
     * repointTargets: already-existing LMS assignments to re-point at what this import creates,
     * instead of creating new ones - see LmsRepointTargets. None for a normal (manual zip upload)
     * import.
     *
     * notifyOwnerOnLmsFailure: email the import's owner if creating or re-pointing its LMS
     * assignments fails - for an import nobody is watching, i.e. one recreating a copied course's
     * experiments in the background (see ExperimentCopyNotificationService).
     *
     * keepSourceTitle: give the new experiment the source experiment's own title, rather than
     * labelling it "(Imported)" - for recreating a copied course's experiments, which should look
     * just as they did in the original course. A manual import is always labelled.
     */
    void process(ExperimentImport experimentImport, SecuredInfo securedInfo, LmsRepointTargets repointTargets, boolean notifyOwnerOnLmsFailure, boolean keepSourceTitle) throws ExperimentImportException;

}
