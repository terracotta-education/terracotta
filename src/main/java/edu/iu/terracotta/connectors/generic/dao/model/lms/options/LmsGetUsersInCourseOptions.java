package edu.iu.terracotta.connectors.generic.dao.model.lms.options;

import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentState;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.enums.EnrollmentType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LmsGetUsersInCourseOptions {

    private String lmsCourseId;
    private List<EnrollmentState> enrollmentState;
    private List<EnrollmentType> enrollmentType;

}
