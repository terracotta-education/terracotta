package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFile;
import edu.ksu.canvas.annotation.CanvasObject;
import edu.ksu.canvas.model.File;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@CanvasObject(postKey = "file")
public class FileExtended extends LmsFile {

    @Builder.Default private File file = new File();

    @Override
    public String getId() {
        return file.getId().toString();
    }

    @Override
    public String getDisplayName() {
        return file.getDisplayName();
    }

    @Override
    public String getFilename() {
        return file.getFilename();
    }

    @Override
    public long getSize() {
        return file.getSize();
    }

    @Override
    public String getUrl() {
        return file.getUrl();
    }

    @Override
    public LmsFile from() {
        return (LmsFile) this;
    }

    public static FileExtended of(LmsFile lmsFile) {
        if (lmsFile == null) {
            return FileExtended.builder().build();
        }

        FileExtended fileExtended = FileExtended.builder().build();
        fileExtended.setDisplayName(lmsFile.getDisplayName());
        fileExtended.setFilename(lmsFile.getFilename());
        fileExtended.setId(lmsFile.getId());
        fileExtended.setSize(lmsFile.getSize());
        fileExtended.setUrl(lmsFile.getUrl());

        return fileExtended;
    }

}
