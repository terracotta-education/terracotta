package edu.iu.terracotta.service.canvas.impl;

import com.google.gson.reflect.TypeToken;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.service.canvas.AssignmentReaderExtended;
import edu.iu.terracotta.service.canvas.AssignmentWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.interfaces.AssignmentReader;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSingleAssignmentOptions;
import edu.ksu.canvas.requestOptions.ListCourseAssignmentsOptions;
import edu.ksu.canvas.requestOptions.ListUserAssignmentOptions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"PMD.GuardLogStatement"})
public class AssignmentExtendedImpl  extends BaseImpl<AssignmentExtended, AssignmentReaderExtended, AssignmentWriterExtended> implements AssignmentReaderExtended, AssignmentWriterExtended {
    private static final Logger LOG = LoggerFactory.getLogger(AssignmentReader.class);

    public AssignmentExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    public List<AssignmentExtended> listCourseAssignments(ListCourseAssignmentsOptions options) throws IOException {
        String url = this.buildCanvasUrl("courses/" + options.getCourseId() + "/assignments", options.getOptionsMap());
        return this.getListFromCanvas(url);
    }

    public List<AssignmentExtended> listUserAssignments(ListUserAssignmentOptions options) throws IOException {
        String url = this.buildCanvasUrl("users/" + options.getUserId() + "/courses/" + options.getCourseId() + "/assignments", options.getOptionsMap());
        return this.getListFromCanvas(url);
    }

    public Optional<AssignmentExtended> getSingleAssignment(GetSingleAssignmentOptions options) throws IOException {
        String url = this.buildCanvasUrl("courses/" + options.getCourseId() + "/assignments/" + options.getAssignmentId(), options.getOptionsMap());
        Response response = this.canvasMessenger.getSingleResponseFromCanvas(this.oauthToken, url);
        return this.responseParser.parseToObject(AssignmentExtended.class, response);
    }

    public Optional<AssignmentExtended> createAssignment(String courseId, AssignmentExtended assignment) throws IOException {
        if (StringUtils.isBlank(assignment.getName())) {
            throw new IllegalArgumentException("Assignment must have a name");
        } else {
            String url = this.buildCanvasUrl("courses/" + courseId + "/assignments", Collections.emptyMap());
            Response response = this.canvasMessenger.sendJsonPostToCanvas(this.oauthToken, url, assignment.toJsonObject(this.serializeNulls));
            return this.responseParser.parseToObject(AssignmentExtended.class, response);
        }
    }

    public Optional<AssignmentExtended> deleteAssignment(String courseId, Integer assignmentId) throws IOException {
        Map<String, List<String>> postParams = new HashMap<>();
        postParams.put("event", Collections.singletonList("delete"));
        String createdUrl = this.buildCanvasUrl("courses/" + courseId + "/assignments/" + assignmentId, Collections.emptyMap());
        Response response = this.canvasMessenger.deleteFromCanvas(this.oauthToken, createdUrl, postParams);
        LOG.debug("response " + response.toString());
        if (!response.getErrorHappened() && response.getResponseCode() == 200) {
            return this.responseParser.parseToObject(AssignmentExtended.class, response);
        } else {
            LOG.debug("Failed to delete assignment, error message: " + response);
            return Optional.empty();
        }
    }

    public Optional<AssignmentExtended> editAssignment(String courseId, AssignmentExtended assignment) throws IOException {
        String url = this.buildCanvasUrl("courses/" + courseId + "/assignments/" + assignment.getId(), Collections.emptyMap());
        Response response = this.canvasMessenger.sendJsonPutToCanvas(this.oauthToken, url, assignment.toJsonObject(this.serializeNulls));
        return this.responseParser.parseToObject(AssignmentExtended.class, response);
    }

    protected Type listType() {
        return (new TypeToken<List<AssignmentExtended>>() {
        }).getType();
    }

    protected Class<AssignmentExtended> objectType() {
        return AssignmentExtended.class;
    }
}
