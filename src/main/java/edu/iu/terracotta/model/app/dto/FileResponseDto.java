package edu.iu.terracotta.model.app.dto;

import java.io.File;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileResponseDto {

    private String fileName;
    private String mimeType;
    private File file;

}
