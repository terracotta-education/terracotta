package edu.iu.terracotta.connectors.brightspace.io.impl;

import java.io.IOException;
import java.util.List;

import edu.iu.terracotta.connectors.brightspace.dao.model.extended.CourseExtended;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.CourseReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.CourseWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;

@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class CourseServiceImpl extends BaseServiceImpl<CourseExtended, CourseReaderService, CourseWriterService> implements CourseReaderService, CourseWriterService {

    public CourseServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public List<CourseExtended> listCoursesForUser(long userId) throws IOException {
        //return parseList(courseImpl.listUserCourses(options));
        return List.of();
    }

    @Override
    public TypeReference<List<CourseExtended>> listType() {
        return new TypeReference<List<CourseExtended>>() {};
    }

    @Override
    public Class<CourseExtended> objectType() {
        return CourseExtended.class;
    }

    /*private List<CourseExtended> parseList(List<Course> courses) {
        List<CourseExtended> courseExtendedList = new ArrayList<>();

        courses.stream()
            .forEach(
                course -> courseExtendedList.add(
                    CourseExtended.builder()
                        .course(course)
                        .type(Course.class)
                        .build()
                )
            );

            return courseExtendedList;
    }*/

}
