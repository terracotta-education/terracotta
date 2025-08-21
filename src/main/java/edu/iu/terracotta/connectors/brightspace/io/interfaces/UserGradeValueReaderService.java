package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.List;

import edu.iu.terracotta.connectors.brightspace.io.model.UserGradeValue;

public interface UserGradeValueReaderService extends BrightspaceReaderService<UserGradeValue, UserGradeValueReaderService> {

    List<UserGradeValue> getAll(String orgUnitId, String gradeObjectId) throws IOException;

}
