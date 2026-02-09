package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.List;

import edu.iu.terracotta.connectors.brightspace.dao.model.extended.UserExtended;
import edu.iu.terracotta.connectors.brightspace.io.model.ClasslistUser;

public interface ClasslistUserReaderService extends BrightspaceReaderService<ClasslistUser, ClasslistUserReaderService> {

    List<UserExtended> getAll(String orgUnitId, boolean onlyShowShownInGrades, Long roleId) throws IOException;

}
