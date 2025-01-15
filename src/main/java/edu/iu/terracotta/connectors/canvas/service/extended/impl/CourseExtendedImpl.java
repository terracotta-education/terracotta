package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.google.common.reflect.TypeToken;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.CourseExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.CourseReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.CourseWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.model.Course;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.ListUserCoursesOptions;

public class CourseExtendedImpl extends BaseImpl<CourseExtended, CourseReaderExtended, CourseWriterExtended> implements CourseReaderExtended, CourseWriterExtended {

    public CourseExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public Optional<CourseExtended> editCourse(String canvasCourseId, CourseExtended courseExtended) throws IOException {
        String url = this.buildCanvasUrl(String.format("courses/%s", canvasCourseId), Collections.emptyMap());

        return parseResponseObjectOptional(this.canvasMessenger.sendJsonPutToCanvas(this.oauthToken, url, courseExtended.getCourse().toJsonObject(this.serializeNulls)));
    }

    @Override
    public List<CourseExtended> listCoursesForUser(ListUserCoursesOptions options) throws IOException {
        String url = this.buildCanvasUrl(String.format("users/%s/courses", options.getUserId()), options.getOptionsMap());

        return parseList(getListResponseFromCanvas(url));
    }

    @Override
    public Type listType() {
        return new TypeToken<List<CourseExtended>>() {}.getType();
    }

    @Override
    public Class<CourseExtended> objectType() {
        return CourseExtended.class;
    }

    private List<Response> getListResponseFromCanvas(String url) throws IOException {
        Consumer<Response> consumer = null;

        if (responseCallback != null) {
            consumer = response -> responseCallback.accept(responseParser.parseToList(listType(), response));
        }

        List<Response> responses = canvasMessenger.getFromCanvas(oauthToken, url, consumer);
        responseCallback = null;

        return responses;
    }

    private List<CourseExtended> parseList(List<Response> responses) {
        List<CourseExtended> courseExtendedList = new ArrayList<>();

        responses.stream()
            .forEach(response -> courseExtendedList.addAll(parseResponseList(response)));

        return courseExtendedList;
    }

    private List<CourseExtended> parseResponseList(Response response) {
        List<CourseExtended> courseExtendedList = responseParser.parseToList(
                listType(),
                response
            );
        List<Course> courseList = responseParser.parseToList(
            new TypeToken<List<Course>>() {}.getType(),
            response
        );

        AtomicInteger index = new AtomicInteger(0);

        return courseExtendedList.stream()
            .map(
                courseExtended -> {
                    courseExtended.setCourse(courseList.get(index.getAndIncrement()));
                    courseExtended.setType(Course.class);

                    return courseExtended;
                }
            )
            .toList();
    }

    /*private List<CourseExtended> parseResponseList(Response response) {
        CourseExtended courseExtended = (CourseExtended) responseParser.parseToList(
            listType(),
            response
        ).get(0);
        courseExtended.setCourse(
            responseParser.parseToList(
                new TypeToken<List<Course>>() {}.getType(),
                response
            )
            .get(0)
        );
        courseExtended.setType(Course.class);

        return Optional.of(courseExtended);
    }*/

    private Optional<CourseExtended> parseResponseObjectOptional(Response response) {
        CourseExtended courseExtended = (CourseExtended) responseParser.parseToObject(
            objectType(),
            response
        ).get();
        courseExtended.setCourse(
            responseParser.parseToObject(
                Course.class,
                response
            )
            .get()
        );
        courseExtended.setType(Course.class);
        courseExtended.setId(courseExtended.getCourse().getId());

        return Optional.of(courseExtended);
    }

}
