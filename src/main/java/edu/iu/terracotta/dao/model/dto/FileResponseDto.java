package edu.iu.terracotta.dao.model.dto;

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
