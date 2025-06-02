package edu.iu.terracotta.dao.model.dto;

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
public class ConfigurationDto {

    @Builder.Default private boolean experimentExportEnabled = false;
    @Builder.Default private boolean messagingEnabled = false;

}
