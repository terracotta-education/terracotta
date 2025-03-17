package edu.iu.terracotta.service.app.distribute;

import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.exceptions.ExperimentExportException;

public interface ExperimentExportService {

    ExportDto export(Experiment experiment) throws ExperimentExportException;

}
