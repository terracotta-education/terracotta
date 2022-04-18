package edu.iu.terracotta.service.canvas.impl;

import edu.iu.terracotta.service.canvas.AssignmentReaderExtended;
import edu.iu.terracotta.service.canvas.AssignmentWriterExtended;
import edu.iu.terracotta.service.canvas.SubmissionReaderExtended;
import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.impl.*;
import edu.ksu.canvas.interfaces.*;
import edu.ksu.canvas.net.RefreshingRestClient;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class CanvasApiFactoryExtended {
    public static final Integer CANVAS_API_VERSION = 1;
    private static final Logger LOG = LoggerFactory.getLogger(CanvasApiFactory.class);
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 120000;
    Map<Class<? extends CanvasReader>, Class<? extends BaseImpl>> readerMap;
    Map<Class<? extends CanvasWriter>, Class<? extends BaseImpl>> writerMap;
    private String canvasBaseUrl;
    private int connectTimeout;
    private int readTimeout;

    public CanvasApiFactoryExtended(String canvasBaseUrl) {
        LOG.debug("Creating Canvas API factory with base URL: " + canvasBaseUrl);
        this.canvasBaseUrl = canvasBaseUrl;
        this.connectTimeout = 5000;
        this.readTimeout = 120000;
        this.setupClassMap();
    }

    public CanvasApiFactoryExtended(String canvasBaseUrl, int connectTimeout, int readTimeout) {
        this.canvasBaseUrl = canvasBaseUrl;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.setupClassMap();
    }

    public <T extends CanvasReader> T getReader(Class<T> type, OauthToken oauthToken) {
        return this.getReader(type, oauthToken, (Integer) null);
    }

    public <T extends CanvasReader> T getReader(Class<T> type, OauthToken oauthToken, Integer paginationPageSize) {
        LOG.debug("Factory call to instantiate class: " + type.getName());
        RestClient restClient = new RefreshingRestClient();
        Class<T> concreteClass = (Class) this.readerMap.get(type);
        if (concreteClass == null) {
            throw new UnsupportedOperationException("No implementation for requested interface found: " + type.getName());
        } else {
            LOG.debug("got class: " + concreteClass);

            try {
                Constructor<T> constructor = concreteClass.getConstructor(String.class, Integer.class, OauthToken.class, RestClient.class, Integer.TYPE, Integer.TYPE, Integer.class, Boolean.class);
                return (T) constructor.newInstance(this.canvasBaseUrl, CANVAS_API_VERSION, oauthToken, restClient, this.connectTimeout, this.readTimeout, paginationPageSize, false);
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException var7) {
                throw new UnsupportedOperationException("Unknown error instantiating the concrete API class: " + type.getName(), var7);
            }
        }
    }

    public <T extends CanvasWriter> T getWriter(Class<T> type, OauthToken oauthToken) {
        return this.getWriter(type, oauthToken, false);
    }

    public <T extends CanvasWriter> T getWriter(Class<T> type, OauthToken oauthToken, Boolean serializeNulls) {
        LOG.debug("Factory call to instantiate class: " + type.getName());
        RestClient restClient = new RefreshingRestClient();
        Class<T> concreteClass = (Class) this.writerMap.get(type);
        if (concreteClass == null) {
            throw new UnsupportedOperationException("No implementation for requested interface found: " + type.getName());
        } else {
            LOG.debug("got writer class: " + concreteClass);

            try {
                Constructor<T> constructor = concreteClass.getConstructor(String.class, Integer.class, OauthToken.class, RestClient.class, Integer.TYPE, Integer.TYPE, Integer.class, Boolean.class);
                return (T) constructor.newInstance(this.canvasBaseUrl, CANVAS_API_VERSION, oauthToken, restClient, this.connectTimeout, this.readTimeout, null, serializeNulls);
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException var7) {
                throw new UnsupportedOperationException("Unknown error instantiating the concrete API class: " + type.getName(), var7);
            }
        }
    }

    private void setupClassMap() {
        this.readerMap = new HashMap();
        this.writerMap = new HashMap();
        this.readerMap.put(AccountReader.class, AccountImpl.class);
        this.readerMap.put(AdminReader.class, AdminImpl.class);
        this.readerMap.put(AssignmentOverrideReader.class, AssignmentOverrideImpl.class);
        this.readerMap.put(AssignmentReader.class, AssignmentImpl.class);
        this.readerMap.put(AssignmentReaderExtended.class, AssignmentExtendedImpl.class);
        this.readerMap.put(ConversationReader.class, ConversationImpl.class);
        this.readerMap.put(CourseReader.class, CourseImpl.class);
        this.readerMap.put(TabReader.class, TabImpl.class);
        this.readerMap.put(EnrollmentReader.class, EnrollmentImpl.class);
        this.readerMap.put(QuizQuestionReader.class, QuizQuestionImpl.class);
        this.readerMap.put(QuizReader.class, QuizImpl.class);
        this.readerMap.put(QuizSubmissionQuestionReader.class, QuizSubmissionQuestionImpl.class);
        this.readerMap.put(QuizSubmissionReader.class, QuizSubmissionImpl.class);
        this.readerMap.put(SectionReader.class, SectionsImpl.class);
        this.readerMap.put(UserReader.class, UserImpl.class);
        this.readerMap.put(PageReader.class, PageImpl.class);
        this.readerMap.put(EnrollmentTermReader.class, EnrollmentTermImpl.class);
        this.readerMap.put(SubmissionReader.class, SubmissionImpl.class);
        this.readerMap.put(SubmissionReaderExtended.class, SubmissionExtendedImpl.class);
        this.readerMap.put(AssignmentGroupReader.class, AssignmentGroupImpl.class);
        this.readerMap.put(RoleReader.class, RoleImpl.class);
        this.readerMap.put(ExternalToolReader.class, ExternalToolImpl.class);
        this.readerMap.put(LoginReader.class, LoginImpl.class);
        this.readerMap.put(CalendarReader.class, CalendarEventImpl.class);
        this.readerMap.put(AccountReportSummaryReader.class, AccountReportSummaryImpl.class);
        this.readerMap.put(AccountReportReader.class, AccountReportImpl.class);
        this.readerMap.put(ContentMigrationReader.class, ContentMigrationImpl.class);
        this.readerMap.put(ProgressReader.class, ProgressImpl.class);
        this.readerMap.put(CourseSettingsReader.class, CourseSettingsImpl.class);
        this.readerMap.put(GradingStandardReader.class, GradingStandardImpl.class);
        this.writerMap.put(AssignmentOverrideWriter.class, AssignmentOverrideImpl.class);
        this.writerMap.put(AdminWriter.class, AdminImpl.class);
        this.writerMap.put(AssignmentWriter.class, AssignmentImpl.class);
        this.writerMap.put(AssignmentWriterExtended.class, AssignmentExtendedImpl.class);
        this.writerMap.put(ConversationWriter.class, ConversationImpl.class);
        this.writerMap.put(CourseWriter.class, CourseImpl.class);
        this.writerMap.put(TabWriter.class, TabImpl.class);
        this.writerMap.put(EnrollmentWriter.class, EnrollmentImpl.class);
        this.writerMap.put(QuizQuestionWriter.class, QuizQuestionImpl.class);
        this.writerMap.put(QuizWriter.class, QuizImpl.class);
        this.writerMap.put(QuizSubmissionQuestionWriter.class, QuizSubmissionQuestionImpl.class);
        this.writerMap.put(QuizSubmissionWriter.class, QuizSubmissionImpl.class);
        this.writerMap.put(UserWriter.class, UserImpl.class);
        this.writerMap.put(PageWriter.class, PageImpl.class);
        this.writerMap.put(SectionWriter.class, SectionsImpl.class);
        this.writerMap.put(SubmissionWriter.class, SubmissionImpl.class);
        this.writerMap.put(AssignmentGroupWriter.class, AssignmentGroupImpl.class);
        this.writerMap.put(RoleWriter.class, RoleImpl.class);
        this.writerMap.put(ExternalToolWriter.class, ExternalToolImpl.class);
        this.writerMap.put(LoginWriter.class, LoginImpl.class);
        this.writerMap.put(CalendarWriter.class, CalendarEventImpl.class);
        this.writerMap.put(AccountReportSummaryWriter.class, AccountReportSummaryImpl.class);
        this.writerMap.put(AccountReportWriter.class, AccountReportImpl.class);
        this.writerMap.put(ContentMigrationWriter.class, ContentMigrationImpl.class);
        this.writerMap.put(ProgressWriter.class, ProgressImpl.class);
        this.writerMap.put(CourseSettingsWriter.class, CourseSettingsImpl.class);
        this.writerMap.put(GradingStandardWriter.class, GradingStandardImpl.class);
    }
}
