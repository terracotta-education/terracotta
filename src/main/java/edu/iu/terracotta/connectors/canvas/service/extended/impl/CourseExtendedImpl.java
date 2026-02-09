package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import com.google.common.reflect.TypeToken;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.CourseExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.CourseReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.CourseWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.impl.CourseImpl;
import edu.ksu.canvas.model.Course;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.ListUserCoursesOptions;

public class CourseExtendedImpl extends BaseImpl<CourseExtended, CourseReaderExtended, CourseWriterExtended> implements CourseReaderExtended, CourseWriterExtended {

    private CourseImpl courseImpl = null;

    public CourseExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.courseImpl = new CourseImpl(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public List<CourseExtended> listCoursesForUser(ListUserCoursesOptions options) throws IOException {
        return parseList(courseImpl.listUserCourses(options));
    }

    @Override
    public Type listType() {
        return new TypeToken<List<CourseExtended>>() {}.getType();
    }

    @Override
    public Class<CourseExtended> objectType() {
        return CourseExtended.class;
    }

    private List<CourseExtended> parseList(List<Course> courses) {
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
    }

}
