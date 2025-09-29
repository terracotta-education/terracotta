package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFolder;
import edu.ksu.canvas.annotation.CanvasObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@CanvasObject(postKey = "folder")
public class FolderExtended extends LmsFolder {

}
