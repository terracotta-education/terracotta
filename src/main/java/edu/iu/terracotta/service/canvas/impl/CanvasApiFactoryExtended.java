package edu.iu.terracotta.service.canvas.impl;

import edu.iu.terracotta.service.canvas.AssignmentReaderExtended;
import edu.iu.terracotta.service.canvas.AssignmentWriterExtended;
import edu.iu.terracotta.service.canvas.CourseReaderExtended;
import edu.iu.terracotta.service.canvas.CourseWriterExtended;
import edu.iu.terracotta.service.canvas.SubmissionReaderExtended;
import edu.ksu.canvas.impl.AccountImpl;
import edu.ksu.canvas.impl.AccountReportImpl;
import edu.ksu.canvas.impl.AccountReportSummaryImpl;
import edu.ksu.canvas.impl.AdminImpl;
import edu.ksu.canvas.impl.AssignmentGroupImpl;
import edu.ksu.canvas.impl.AssignmentImpl;
import edu.ksu.canvas.impl.AssignmentOverrideImpl;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.impl.CalendarEventImpl;
import edu.ksu.canvas.impl.ContentMigrationImpl;
import edu.ksu.canvas.impl.ConversationImpl;
import edu.ksu.canvas.impl.CourseImpl;
import edu.ksu.canvas.impl.CourseSettingsImpl;
import edu.ksu.canvas.impl.EnrollmentImpl;
import edu.ksu.canvas.impl.EnrollmentTermImpl;
import edu.ksu.canvas.impl.ExternalToolImpl;
import edu.ksu.canvas.impl.GradingStandardImpl;
import edu.ksu.canvas.impl.LoginImpl;
import edu.ksu.canvas.impl.PageImpl;
import edu.ksu.canvas.impl.ProgressImpl;
import edu.ksu.canvas.impl.QuizImpl;
import edu.ksu.canvas.impl.QuizQuestionImpl;
import edu.ksu.canvas.impl.QuizSubmissionImpl;
import edu.ksu.canvas.impl.QuizSubmissionQuestionImpl;
import edu.ksu.canvas.impl.RoleImpl;
import edu.ksu.canvas.impl.SectionsImpl;
import edu.ksu.canvas.impl.SubmissionImpl;
import edu.ksu.canvas.impl.TabImpl;
import edu.ksu.canvas.impl.UserImpl;
import edu.ksu.canvas.interfaces.AccountReader;
import edu.ksu.canvas.interfaces.AccountReportReader;
import edu.ksu.canvas.interfaces.AccountReportSummaryReader;
import edu.ksu.canvas.interfaces.AccountReportSummaryWriter;
import edu.ksu.canvas.interfaces.AccountReportWriter;
import edu.ksu.canvas.interfaces.AdminReader;
import edu.ksu.canvas.interfaces.AdminWriter;
import edu.ksu.canvas.interfaces.AssignmentGroupReader;
import edu.ksu.canvas.interfaces.AssignmentGroupWriter;
import edu.ksu.canvas.interfaces.AssignmentOverrideReader;
import edu.ksu.canvas.interfaces.AssignmentOverrideWriter;
import edu.ksu.canvas.interfaces.AssignmentReader;
import edu.ksu.canvas.interfaces.AssignmentWriter;
import edu.ksu.canvas.interfaces.CalendarReader;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.interfaces.CanvasReader;
import edu.ksu.canvas.interfaces.CanvasWriter;
import edu.ksu.canvas.interfaces.ContentMigrationReader;
import edu.ksu.canvas.interfaces.ContentMigrationWriter;
import edu.ksu.canvas.interfaces.ConversationReader;
import edu.ksu.canvas.interfaces.ConversationWriter;
import edu.ksu.canvas.interfaces.CourseReader;
import edu.ksu.canvas.interfaces.CourseSettingsReader;
import edu.ksu.canvas.interfaces.CourseSettingsWriter;
import edu.ksu.canvas.interfaces.CourseWriter;
import edu.ksu.canvas.interfaces.EnrollmentReader;
import edu.ksu.canvas.interfaces.EnrollmentTermReader;
import edu.ksu.canvas.interfaces.EnrollmentWriter;
import edu.ksu.canvas.interfaces.ExternalToolReader;
import edu.ksu.canvas.interfaces.ExternalToolWriter;
import edu.ksu.canvas.interfaces.GradingStandardReader;
import edu.ksu.canvas.interfaces.GradingStandardWriter;
import edu.ksu.canvas.interfaces.LoginReader;
import edu.ksu.canvas.interfaces.LoginWriter;
import edu.ksu.canvas.interfaces.PageReader;
import edu.ksu.canvas.interfaces.PageWriter;
import edu.ksu.canvas.interfaces.ProgressReader;
import edu.ksu.canvas.interfaces.ProgressWriter;
import edu.ksu.canvas.interfaces.QuizQuestionReader;
import edu.ksu.canvas.interfaces.QuizQuestionWriter;
import edu.ksu.canvas.interfaces.QuizReader;
import edu.ksu.canvas.interfaces.QuizSubmissionQuestionReader;
import edu.ksu.canvas.interfaces.QuizSubmissionQuestionWriter;
import edu.ksu.canvas.interfaces.QuizSubmissionReader;
import edu.ksu.canvas.interfaces.QuizSubmissionWriter;
import edu.ksu.canvas.interfaces.QuizWriter;
import edu.ksu.canvas.interfaces.RoleReader;
import edu.ksu.canvas.interfaces.RoleWriter;
import edu.ksu.canvas.interfaces.SectionReader;
import edu.ksu.canvas.interfaces.SectionWriter;
import edu.ksu.canvas.interfaces.SubmissionReader;
import edu.ksu.canvas.interfaces.SubmissionWriter;
import edu.ksu.canvas.interfaces.TabReader;
import edu.ksu.canvas.interfaces.TabWriter;
import edu.ksu.canvas.interfaces.UserReader;
import edu.ksu.canvas.interfaces.UserWriter;
import edu.ksu.canvas.net.RefreshingRestClient;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"PMD.GuardLogStatement", "rawtypes", "unchecked"})
public class CanvasApiFactoryExtended {

    public static final Integer CANVAS_API_VERSION = 1;

    public Map<Class<? extends CanvasReader>, Class<? extends BaseImpl>> readerMap;
    public Map<Class<? extends CanvasWriter>, Class<? extends BaseImpl>> writerMap;
    private String canvasBaseUrl;
    private int connectTimeout;
    private int readTimeout;

    public CanvasApiFactoryExtended(String canvasBaseUrl) {
        this.canvasBaseUrl = canvasBaseUrl;
        connectTimeout = 5000;
        readTimeout = 120000;
        setupClassMap();
    }

    public CanvasApiFactoryExtended(String canvasBaseUrl, int connectTimeout, int readTimeout) {
        this.canvasBaseUrl = canvasBaseUrl;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        setupClassMap();
    }

    public <T extends CanvasReader> T getReader(Class<T> type, OauthToken oauthToken) {
        return getReader(type, oauthToken, (Integer) null);
    }

    public <T extends CanvasReader> T getReader(Class<T> type, OauthToken oauthToken, Integer paginationPageSize) {
        RestClient restClient = new RefreshingRestClient();
        Class<T> concreteClass = (Class) this.readerMap.get(type);

        if (concreteClass == null) {
            throw new UnsupportedOperationException("No implementation for requested interface found: " + type.getName());
        }

        try {
            Constructor<T> constructor = concreteClass.getConstructor(String.class, Integer.class, OauthToken.class, RestClient.class, Integer.TYPE, Integer.TYPE, Integer.class, Boolean.class);

            return (T) constructor.newInstance(this.canvasBaseUrl, CANVAS_API_VERSION, oauthToken, restClient, this.connectTimeout, this.readTimeout, paginationPageSize, false);
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException var7) {
            throw new UnsupportedOperationException("Unknown error instantiating the concrete API class: " + type.getName(), var7);
        }
    }

    public <T extends CanvasWriter> T getWriter(Class<T> type, OauthToken oauthToken) {
        return this.getWriter(type, oauthToken, false);
    }

    public <T extends CanvasWriter> T getWriter(Class<T> type, OauthToken oauthToken, Boolean serializeNulls) {
        RestClient restClient = new RefreshingRestClient();
        Class<T> concreteClass = (Class) this.writerMap.get(type);

        if (concreteClass == null) {
            throw new UnsupportedOperationException("No implementation for requested interface found: " + type.getName());
        }

        try {
            Constructor<T> constructor = concreteClass.getConstructor(String.class, Integer.class, OauthToken.class, RestClient.class, Integer.TYPE, Integer.TYPE, Integer.class, Boolean.class);
            return (T) constructor.newInstance(this.canvasBaseUrl, CANVAS_API_VERSION, oauthToken, restClient, this.connectTimeout, this.readTimeout, null, serializeNulls);
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException var7) {
            throw new UnsupportedOperationException("Unknown error instantiating the concrete API class: " + type.getName(), var7);
        }
    }

    private void setupClassMap() {
        readerMap = new HashMap<>();
        writerMap = new HashMap<>();
        readerMap.put(AccountReader.class, AccountImpl.class);
        readerMap.put(AdminReader.class, AdminImpl.class);
        readerMap.put(AssignmentOverrideReader.class, AssignmentOverrideImpl.class);
        readerMap.put(AssignmentReader.class, AssignmentImpl.class);
        readerMap.put(AssignmentReaderExtended.class, AssignmentExtendedImpl.class);
        readerMap.put(ConversationReader.class, ConversationImpl.class);
        readerMap.put(CourseReader.class, CourseImpl.class);
        readerMap.put(TabReader.class, TabImpl.class);
        readerMap.put(EnrollmentReader.class, EnrollmentImpl.class);
        readerMap.put(QuizQuestionReader.class, QuizQuestionImpl.class);
        readerMap.put(QuizReader.class, QuizImpl.class);
        readerMap.put(QuizSubmissionQuestionReader.class, QuizSubmissionQuestionImpl.class);
        readerMap.put(QuizSubmissionReader.class, QuizSubmissionImpl.class);
        readerMap.put(SectionReader.class, SectionsImpl.class);
        readerMap.put(UserReader.class, UserImpl.class);
        readerMap.put(PageReader.class, PageImpl.class);
        readerMap.put(EnrollmentTermReader.class, EnrollmentTermImpl.class);
        readerMap.put(SubmissionReader.class, SubmissionImpl.class);
        readerMap.put(SubmissionReaderExtended.class, SubmissionExtendedImpl.class);
        readerMap.put(AssignmentGroupReader.class, AssignmentGroupImpl.class);
        readerMap.put(RoleReader.class, RoleImpl.class);
        readerMap.put(ExternalToolReader.class, ExternalToolImpl.class);
        readerMap.put(LoginReader.class, LoginImpl.class);
        readerMap.put(CalendarReader.class, CalendarEventImpl.class);
        readerMap.put(AccountReportSummaryReader.class, AccountReportSummaryImpl.class);
        readerMap.put(AccountReportReader.class, AccountReportImpl.class);
        readerMap.put(ContentMigrationReader.class, ContentMigrationImpl.class);
        readerMap.put(ProgressReader.class, ProgressImpl.class);
        readerMap.put(CourseSettingsReader.class, CourseSettingsImpl.class);
        readerMap.put(GradingStandardReader.class, GradingStandardImpl.class);
        readerMap.put(CourseReaderExtended.class, CourseExtendedImpl.class);
        writerMap.put(AssignmentOverrideWriter.class, AssignmentOverrideImpl.class);
        writerMap.put(AdminWriter.class, AdminImpl.class);
        writerMap.put(AssignmentWriter.class, AssignmentImpl.class);
        writerMap.put(AssignmentWriterExtended.class, AssignmentExtendedImpl.class);
        writerMap.put(ConversationWriter.class, ConversationImpl.class);
        writerMap.put(CourseWriter.class, CourseImpl.class);
        writerMap.put(TabWriter.class, TabImpl.class);
        writerMap.put(EnrollmentWriter.class, EnrollmentImpl.class);
        writerMap.put(QuizQuestionWriter.class, QuizQuestionImpl.class);
        writerMap.put(QuizWriter.class, QuizImpl.class);
        writerMap.put(QuizSubmissionQuestionWriter.class, QuizSubmissionQuestionImpl.class);
        writerMap.put(QuizSubmissionWriter.class, QuizSubmissionImpl.class);
        writerMap.put(UserWriter.class, UserImpl.class);
        writerMap.put(PageWriter.class, PageImpl.class);
        writerMap.put(SectionWriter.class, SectionsImpl.class);
        writerMap.put(SubmissionWriter.class, SubmissionImpl.class);
        writerMap.put(AssignmentGroupWriter.class, AssignmentGroupImpl.class);
        writerMap.put(RoleWriter.class, RoleImpl.class);
        writerMap.put(ExternalToolWriter.class, ExternalToolImpl.class);
        writerMap.put(LoginWriter.class, LoginImpl.class);
        writerMap.put(CalendarWriter.class, CalendarEventImpl.class);
        writerMap.put(AccountReportSummaryWriter.class, AccountReportSummaryImpl.class);
        writerMap.put(AccountReportWriter.class, AccountReportImpl.class);
        writerMap.put(ContentMigrationWriter.class, ContentMigrationImpl.class);
        writerMap.put(ProgressWriter.class, ProgressImpl.class);
        writerMap.put(CourseSettingsWriter.class, CourseSettingsImpl.class);
        writerMap.put(GradingStandardWriter.class, GradingStandardImpl.class);
        writerMap.put(CourseWriterExtended.class, CourseExtendedImpl.class);
    }

}
