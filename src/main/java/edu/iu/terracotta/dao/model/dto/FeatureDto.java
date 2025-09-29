package edu.iu.terracotta.dao.model.dto;

import edu.iu.terracotta.dao.model.enums.FeatureType;
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
public class FeatureDto {

    private FeatureType type;

}
