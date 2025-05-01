package edu.iu.terracotta.dao.model.dto.distribute;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportDto {

    @Builder.Default private String mimeType = "application/zip";

    private String filename;
    private File file;

}
