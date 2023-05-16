package edu.iu.terracotta.service.canvas.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.reflect.TypeToken;

import edu.iu.terracotta.model.canvas.CourseExtended;
import edu.iu.terracotta.service.canvas.CourseReaderExtended;
import edu.iu.terracotta.service.canvas.CourseWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
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
        String url = this.buildCanvasUrl("courses/" + canvasCourseId, Collections.emptyMap());
        Response response = this.canvasMessenger.sendJsonPutToCanvas(this.oauthToken, url, courseExtended.toJsonObject(this.serializeNulls));

        return this.responseParser.parseToObject(CourseExtended.class, response);
    }

    @Override
    public List<CourseExtended> listCoursesForUser(ListUserCoursesOptions options) throws IOException {
        String url = this.buildCanvasUrl("users/" + options.getUserId() + "/courses", options.getOptionsMap());

        return this.getListFromCanvas(url);
    }

    @Override
    public Type listType() {
        return new TypeToken<List<CourseExtended>>() {}.getType();
    }

    @Override
    public Class<CourseExtended> objectType() {
        return CourseExtended.class;
    }

}
