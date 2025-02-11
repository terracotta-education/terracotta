package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import com.google.gson.reflect.TypeToken;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.AssignmentExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.AssignmentReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.AssignmentWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.model.assignment.Assignment;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetSingleAssignmentOptions;
import edu.ksu.canvas.requestOptions.ListCourseAssignmentsOptions;
import edu.ksu.canvas.requestOptions.ListUserAssignmentOptions;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class AssignmentExtendedImpl extends BaseImpl<AssignmentExtended, AssignmentReaderExtended, AssignmentWriterExtended> implements AssignmentReaderExtended, AssignmentWriterExtended {

    public AssignmentExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    public List<AssignmentExtended> listCourseAssignments(ListCourseAssignmentsOptions options) throws IOException {
        String url = buildCanvasUrl(
            String.format(
                "courses/%s/assignments",
                options.getCourseId()
            ),
            options.getOptionsMap()
        );

        return parseList(getListResponseFromCanvas(url));
    }

    public List<AssignmentExtended> listUserAssignments(ListUserAssignmentOptions options) throws IOException {
        String url = buildCanvasUrl(
            String.format(
                "users/%s/courses/%s/assignments",
                options.getUserId(),
                options.getCourseId()
            ),
            options.getOptionsMap()
        );

        return parseList(getListResponseFromCanvas(url));
    }

    public Optional<AssignmentExtended> getSingleAssignment(GetSingleAssignmentOptions options) throws IOException {
        String url = buildCanvasUrl(
            String.format(
                "courses/%s/assignments/%s",
                options.getCourseId(),
                options.getAssignmentId()
            ),
            options.getOptionsMap()
        );

        return parseResponseObjectOptional(canvasMessenger.getSingleResponseFromCanvas(oauthToken, url));
    }

    public Optional<AssignmentExtended> createAssignment(String courseId, Assignment assignment) throws IOException {
        if (StringUtils.isBlank(assignment.getName())) {
            throw new IllegalArgumentException("Assignment must have a name");
        }

        String url = buildCanvasUrl(
            String.format(
                "courses/%s/assignments",
                courseId
            ),
            Collections.emptyMap()
        );
        Response response = canvasMessenger.sendJsonPostToCanvas(oauthToken, url, assignment.toJsonObject(serializeNulls));

        return parseResponseObjectOptional(response);
    }

    public Optional<AssignmentExtended> deleteAssignment(String courseId, Long canvasAssignmentId) throws IOException {
        Map<String, List<String>> postParams = new HashMap<>();
        postParams.put("event", Collections.singletonList("delete"));
        String createdUrl = buildCanvasUrl(
            String.format(
                "courses/%s/assignments/%s",
                courseId,
                canvasAssignmentId
            ),
            Collections.emptyMap()
        );
        Response response = canvasMessenger.deleteFromCanvas(oauthToken, createdUrl, postParams);
        log.debug("response [{}]", response.toString());

        if (!response.getErrorHappened() && response.getResponseCode() == 200) {
            return parseResponseObjectOptional(response);
        }

        log.debug("Failed to delete assignment, error message: {}",response);
        return Optional.empty();
    }

    public Optional<AssignmentExtended> editAssignment(String courseId, Assignment assignment) throws IOException {
        String url = buildCanvasUrl(
            String.format(
                "courses/%s/assignments/%s",
                courseId,
                assignment.getId()
            ),
            Collections.emptyMap()
        );

        return parseResponseObjectOptional(canvasMessenger.sendJsonPutToCanvas(oauthToken, url, assignment.toJsonObject(serializeNulls)));
    }

    protected Type listType() {
        return new TypeToken<List<AssignmentExtended>>() {}.getType();
    }

    protected Class<AssignmentExtended> objectType() {
        return AssignmentExtended.class;
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

    private List<AssignmentExtended> parseList(List<Response> responses) {
        List<AssignmentExtended> assignmentExtendedList = new ArrayList<>();

        responses.stream()
            .forEach(response -> assignmentExtendedList.addAll(parseResponseList(response)));

        return assignmentExtendedList;
    }

    private List<AssignmentExtended> parseResponseList(Response response) {
        List<AssignmentExtended> assignmentExtendedList = responseParser.parseToList(
                listType(),
                response
            );
        List<Assignment> assignmentList = responseParser.parseToList(
            new TypeToken<List<Assignment>>() {}.getType(),
            response
        );

        AtomicInteger index = new AtomicInteger(0);

        return assignmentExtendedList.stream()
            .map(
                assignmentExtended -> {
                    assignmentExtended.setAssignment(assignmentList.get(index.getAndIncrement()));
                    assignmentExtended.setType(Assignment.class);

                    return assignmentExtended;
                }
            )
            .toList();
    }

    private Optional<AssignmentExtended> parseResponseObjectOptional(Response response) {
        AssignmentExtended assignmentExtended = (AssignmentExtended) responseParser.parseToObject(
            objectType(),
            response
        ).get();
        assignmentExtended.setAssignment(
            responseParser.parseToObject(
                Assignment.class,
                response
            )
            .get()
        );
        assignmentExtended.setType(Assignment.class);

        return Optional.of(assignmentExtended);
    }

}
