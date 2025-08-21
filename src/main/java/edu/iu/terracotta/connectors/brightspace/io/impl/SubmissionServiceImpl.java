package edu.iu.terracotta.connectors.brightspace.io.impl;

import edu.iu.terracotta.connectors.brightspace.dao.model.extended.SubmissionExtended;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.SubmissionReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.SubmissionWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;

public class SubmissionServiceImpl extends BaseServiceImpl<SubmissionExtended, SubmissionReaderService, SubmissionWriterService> implements SubmissionReaderService, SubmissionWriterService {

    public SubmissionServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public List<SubmissionExtended> listSubmissionsForMultipleAssignments(String orgUnitId, List<Long> assignmentIds) throws IOException {
        return List.of();
    }

    @Override
    public List<SubmissionExtended> getCourseSubmissions(String orgUnitId) throws IOException {
        return List.of();
    }

    @Override
    protected TypeReference<List<SubmissionExtended>> listType() {
        return new TypeReference<List<SubmissionExtended>>() {};
    }

    @Override
    protected Class<SubmissionExtended> objectType() {
        return SubmissionExtended.class;
    }

}
