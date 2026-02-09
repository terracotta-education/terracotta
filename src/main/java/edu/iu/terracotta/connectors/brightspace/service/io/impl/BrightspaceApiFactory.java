package edu.iu.terracotta.connectors.brightspace.service.io.impl;

import edu.iu.terracotta.connectors.brightspace.io.impl.AssignmentServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.BaseServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.ClasslistUserServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.ContentObjectModuleServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.ContentObjectTopicServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.CourseServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.DropboxFolderServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.GradeObjectServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.LtiAdvantageLinkServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.LtiAdvantageQuickLinkServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.SubmissionServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.UserGradeValueServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.AssignmentReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.AssignmentWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ClasslistUserReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ClasslistUserWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectModuleReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectModuleWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectTopicReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectTopicWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.CourseReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.CourseWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.DropboxFolderReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.DropboxFolderWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.GradeObjectReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.GradeObjectWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageLinkReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageLinkWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageQuickLinkReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageQuickLinkWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.SubmissionReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.SubmissionWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.UserGradeValueReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.UserGradeValueWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.impl.RefreshingRestClient;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked", "PMD.LooseCoupling"})
public class BrightspaceApiFactory {

    public Map<Class<? extends BrightspaceReaderService>, Class<? extends BaseServiceImpl>> readerMap;
    public Map<Class<? extends BrightspaceWriterService>, Class<? extends BaseServiceImpl>> writerMap;

    private String brightspaceBaseUrl;
    private int connectTimeout;
    private int readTimeout;
    private ApiVersion apiVersion;

    public BrightspaceApiFactory(String brightspaceBaseUrl, ApiVersion apiVersion) {
        this(brightspaceBaseUrl, 5000, 120000, apiVersion);
    }

    public BrightspaceApiFactory(String brightspaceBaseUrl, int connectTimeout, int readTimeout, ApiVersion apiVersion) {
        this.brightspaceBaseUrl = brightspaceBaseUrl;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.apiVersion = apiVersion;
        setupClassMap();
    }

    public <T extends BrightspaceReaderService> T getReader(Class<T> type, OauthToken oauthToken) {
        return getReader(type, oauthToken, (Integer) null);
    }

    public <T extends BrightspaceReaderService> T getReader(Class<T> type, OauthToken oauthToken, Integer paginationPageSize) {
        Class<T> concreteClass = (Class) this.readerMap.get(type);

        if (concreteClass == null) {
            throw new UnsupportedOperationException(String.format("No implementation for requested interface found: [%s]", type.getName()));
        }

        try {
            return (T) concreteClass.getConstructor(
                String.class,
                ApiVersion.class,
                OauthToken.class,
                RestClient.class,
                Integer.TYPE,
                Integer.TYPE,
                Integer.class,
                Boolean.class
            )
            .newInstance(
                this.brightspaceBaseUrl,
                apiVersion,
                oauthToken,
                new RefreshingRestClient(),
                this.connectTimeout,
                this.readTimeout,
                paginationPageSize,
                false
            );
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException e) {
            throw new UnsupportedOperationException(String.format("Unknown error instantiating the concrete API class: [%s]", type.getName()), e);
        }
    }

    public <T extends BrightspaceWriterService> T getWriter(Class<T> type, OauthToken oauthToken) {
        return this.getWriter(type, oauthToken, false);
    }

    public <T extends BrightspaceWriterService> T getWriter(Class<T> type, OauthToken oauthToken, Boolean serializeNulls) {
        Class<T> concreteClass = (Class) this.writerMap.get(type);

        if (concreteClass == null) {
            throw new UnsupportedOperationException(String.format("No implementation for requested interface found: [%s]", type.getName()));
        }

        try {
            return (T) concreteClass.getConstructor(
                String.class,
                ApiVersion.class,
                OauthToken.class,
                RestClient.class,
                Integer.TYPE,
                Integer.TYPE,
                Integer.class,
                Boolean.class
            )
            .newInstance(
                this.brightspaceBaseUrl,
                apiVersion,
                oauthToken,
                new RefreshingRestClient(),
                this.connectTimeout,
                this.readTimeout,
                null,
                serializeNulls
            );
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException e) {
            throw new UnsupportedOperationException(String.format("Unknown error instantiating the concrete API class: [%s]", type.getName()), e);
        }
    }

    /**
     * Creates reader and writer mappings that map interfaces to their concrete implementations
     */
    private void setupClassMap() {
        readerMap = new HashMap<>();
        readerMap.put(AssignmentReaderService.class, AssignmentServiceImpl.class);
        readerMap.put(ClasslistUserReaderService.class, ClasslistUserServiceImpl.class);
        readerMap.put(ContentObjectModuleReaderService.class, ContentObjectModuleServiceImpl.class);
        readerMap.put(ContentObjectTopicReaderService.class, ContentObjectTopicServiceImpl.class);
        readerMap.put(CourseReaderService.class, CourseServiceImpl.class);
        readerMap.put(DropboxFolderReaderService.class, DropboxFolderServiceImpl.class);
        readerMap.put(GradeObjectReaderService.class, GradeObjectServiceImpl.class);
        readerMap.put(LtiAdvantageLinkReaderService.class, LtiAdvantageLinkServiceImpl.class);
        readerMap.put(LtiAdvantageQuickLinkReaderService.class, LtiAdvantageQuickLinkServiceImpl.class);
        readerMap.put(SubmissionReaderService.class, SubmissionServiceImpl.class);
        readerMap.put(UserGradeValueReaderService.class, UserGradeValueServiceImpl.class);

        writerMap = new HashMap<>();
        writerMap.put(AssignmentWriterService.class, AssignmentServiceImpl.class);
        writerMap.put(ClasslistUserWriterService.class, ClasslistUserServiceImpl.class);
        writerMap.put(ContentObjectModuleWriterService.class, ContentObjectModuleServiceImpl.class);
        writerMap.put(ContentObjectTopicWriterService.class, ContentObjectTopicServiceImpl.class);
        writerMap.put(CourseWriterService.class, CourseServiceImpl.class);
        writerMap.put(DropboxFolderWriterService.class, DropboxFolderServiceImpl.class);
        writerMap.put(GradeObjectWriterService.class, GradeObjectServiceImpl.class);
        writerMap.put(LtiAdvantageLinkWriterService.class, LtiAdvantageLinkServiceImpl.class);
        writerMap.put(LtiAdvantageQuickLinkWriterService.class, LtiAdvantageQuickLinkServiceImpl.class);
        writerMap.put(SubmissionWriterService.class, SubmissionServiceImpl.class);
        writerMap.put(UserGradeValueWriterService.class, UserGradeValueServiceImpl.class);
    }

}
