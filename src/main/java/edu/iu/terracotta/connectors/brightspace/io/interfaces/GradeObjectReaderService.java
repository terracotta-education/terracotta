package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.model.GradeObject;

public interface GradeObjectReaderService extends BrightspaceReaderService<GradeObject, GradeObjectReaderService> {

    List<GradeObject> getAll(String orgUnitId) throws IOException;
    Optional<GradeObject> getLatest(String orgUnitId) throws IOException;
    Optional<GradeObject> get(String orgUnitId, long gradeObjectId) throws IOException;

}
