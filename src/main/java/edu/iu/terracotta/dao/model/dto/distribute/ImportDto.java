package edu.iu.terracotta.dao.model.dto.distribute;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
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
public class ImportDto {

    private UUID id;
    private ExperimentImportStatus status;
    private List<ExperimentImportErrorDto> errorMessages;
    private String sourceTitle;
    private String importedTitle;

}
