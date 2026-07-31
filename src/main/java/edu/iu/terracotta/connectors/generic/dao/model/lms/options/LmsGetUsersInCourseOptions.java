package edu.iu.terracotta.connectors.generic.dao.model.lms.options;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentState;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentType;
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
public class LmsGetUsersInCourseOptions {

    private String lmsCourseId;
    private List<EnrollmentState> enrollmentState;
    private List<EnrollmentType> enrollmentType;
    private Integer batchSize;
    private UUID batchId;
    private Long contextId;

}
