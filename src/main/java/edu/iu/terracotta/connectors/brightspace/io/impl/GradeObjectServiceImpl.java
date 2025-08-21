package edu.iu.terracotta.connectors.brightspace.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.collections4.CollectionUtils;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.GradeObjectReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.GradeObjectWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.GradeObject;
import edu.iu.terracotta.connectors.brightspace.io.model.GradeObjectUpdate;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.api.BrightspaceUrl;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import tools.jackson.core.type.TypeReference;

public class GradeObjectServiceImpl extends BaseServiceImpl<GradeObject, GradeObjectReaderService, GradeObjectWriterService> implements GradeObjectReaderService, GradeObjectWriterService {

    public GradeObjectServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public List<GradeObject> getAll(String orgUnitId) throws IOException {
        return parseList(
            getListResponse(
                buildUrl(
                    String.format(
                        BrightspaceUrl.GRADE_OBJECTS_ROOT.url(),
                        apiVersion.getLe(),
                        orgUnitId
                    )
                )
            )
        );
    }

    @Override
    public Optional<GradeObject> getLatest(String orgUnitId) throws IOException {
        List<GradeObject> gradeObjects = getAll(orgUnitId);

        if (CollectionUtils.isEmpty(gradeObjects)) {
            return Optional.empty();
        }

        // sort by ID descending
        gradeObjects.sort((g1, g2) -> g2.getId().compareTo(g1.getId()));

        return gradeObjects.stream().findFirst();
    }

    @Override
    public Optional<GradeObject> get(String orgUnitId, long gradeObjectId) throws IOException {
        return responseParser.parseToObject(
            GradeObject.class,
            brightspaceMessenger.getSingleResponse(
                oauthToken,
                buildUrl(
                    String.format(
                        BrightspaceUrl.GRADE_OBJECT.url(),
                        apiVersion.getLe(),
                        orgUnitId,
                        gradeObjectId
                    )
                )
            )
        );
    }

    @Override
    public Optional<GradeObject> update(String orgUnitId, long gradeObjectId, GradeObjectUpdate gradeObjectUpdate) throws IOException {
        return responseParser.parseToObject(
            GradeObject.class,
            brightspaceMessenger.sendJsonPut(
                oauthToken,
                buildUrl(
                    String.format(
                        BrightspaceUrl.GRADE_OBJECT.url(),
                        apiVersion.getLe(),
                        orgUnitId,
                        gradeObjectId
                    )
                ),
                gradeObjectUpdate.toJson(serializeNulls)
            )
        );
    }

    @Override
    public void delete(String orgUnitId, long gradeObjectId) throws IOException {
        brightspaceMessenger.delete(
            oauthToken,
            buildUrl(
                String.format(
                    BrightspaceUrl.GRADE_OBJECT.url(),
                    apiVersion.getLe(),
                    orgUnitId,
                    gradeObjectId
                )
            )
        );
    }

    @Override
    protected TypeReference<List<GradeObject>> listType() {
        return new TypeReference<List<GradeObject>>() {};
    }

    @Override
    protected Class<GradeObject> objectType() {
        return GradeObject.class;
    }

    private List<Response> getListResponse(String url) throws IOException {
        Consumer<Response> consumer = null;

        if (responseCallback != null) {
            consumer = response -> responseCallback.accept(responseParser.parseToList(listType(), response));
        }

        List<Response> responses = brightspaceMessenger.get(oauthToken, url, consumer);
        responseCallback = null;

        return responses;
    }

    private List<GradeObject> parseList(List<Response> responses) {
        List<GradeObject> gradeObjectList = new ArrayList<>();

        responses.stream()
            .forEach(response -> gradeObjectList.addAll(parseResponseList(response)));

        return gradeObjectList;
    }

    private List<GradeObject> parseResponseList(Response response) {
        return responseParser.parseToList(
            listType(),
            response
        );
    }

}
