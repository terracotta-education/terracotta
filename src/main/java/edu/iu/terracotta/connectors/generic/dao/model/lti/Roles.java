package edu.iu.terracotta.connectors.generic.dao.model.lti;

import com.google.common.collect.ImmutableList;
import edu.iu.terracotta.utils.LtiStrings;

import java.util.List;

// do this as a class instead of an enum so its easier to reuse values
// in annotations, should it ever come to that (as is typical in controller-based auth)
public class Roles {

    public static final String GENERAL = LtiStrings.LTI_ROLE_GENERAL;
    public static final String LEARNER = LtiStrings.LTI_ROLE_LEARNER;
    public static final String INSTRUCTOR = LtiStrings.LTI_ROLE_INSTRUCTOR;
    public static final String MEMBERSHIP_INSTRUCTOR = LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR;
    public static final String MEMBERSHIP_LEARNER = LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER;
    public static final String ADMIN = LtiStrings.LTI_ROLE_ADMIN;
    public static final String TEST_STUDENT = LtiStrings.LTI_ROLE_TEST_STUDENT;

    public static final List<String> GENERAL_ROLE_LIST = ImmutableList.of(GENERAL);
    public static final List<String> STUDENT_ROLE_LIST = ImmutableList.of(MEMBERSHIP_LEARNER);
    public static final List<String> INSTRUCTOR_ROLE_LIST = ImmutableList.of(MEMBERSHIP_INSTRUCTOR);
    public static final List<String> ADMIN_ROLE_LIST = ImmutableList.of(ADMIN);
    public static final List<String> TEST_ROLE_LIST = ImmutableList.of(TEST_STUDENT);

}
