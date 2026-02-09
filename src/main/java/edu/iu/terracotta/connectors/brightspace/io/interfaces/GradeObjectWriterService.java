package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.model.GradeObject;
import edu.iu.terracotta.connectors.brightspace.io.model.GradeObjectUpdate;

public interface GradeObjectWriterService extends BrightspaceWriterService<GradeObject, GradeObjectWriterService> {

    Optional<GradeObject> update(String orgUnitId, long gradeObjectId, GradeObjectUpdate gradeObjectUpdate) throws IOException;
    void delete(String orgUnitId, long gradeObjectId) throws IOException;

}
