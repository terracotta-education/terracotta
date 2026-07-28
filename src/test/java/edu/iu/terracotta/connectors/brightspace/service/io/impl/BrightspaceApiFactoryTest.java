package edu.iu.terracotta.connectors.brightspace.service.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.config.SpringContext;
import edu.iu.terracotta.connectors.brightspace.configuration.BrightspaceConfigurationService;
import edu.iu.terracotta.connectors.brightspace.io.impl.AssignmentServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.CourseServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.DropboxFolderServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.impl.GradeObjectServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.AssignmentReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.AssignmentWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.CourseReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.CourseWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.DropboxFolderReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.DropboxFolderWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.GradeObjectReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.GradeObjectWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

@ExtendWith(MockitoExtension.class)
public class BrightspaceApiFactoryTest {

    private static final String BASE_URL = "https://example.brightspace.com";

    @Mock private OauthToken oauthToken;

    private ApiVersion apiVersion;

    @BeforeEach
    void beforeEach() {
        apiVersion = ApiVersion.builder().le("1.40").lp("1.30").build();

        // getReader/getWriter construct a real RefreshingRestClient, whose field initializer
        // constructs a real SimpleRestClient(), which reaches into SpringContext.getBean(...) -
        // stub the static Spring context so that no-arg construction doesn't NPE outside a container
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        BrightspaceConfigurationService brightspaceConfigurationService = mock(BrightspaceConfigurationService.class);
        lenient().when(applicationContext.getBean(eq(BrightspaceConfigurationService.class))).thenReturn(brightspaceConfigurationService);
        ReflectionTestUtils.setField(SpringContext.class, "context", applicationContext);
    }

    /* =============================== constructors / class maps =============================== */

    @Test
    void testTwoArgConstructor_populatesReaderAndWriterMaps() {
        BrightspaceApiFactory factory = new BrightspaceApiFactory(BASE_URL, apiVersion);

        assertNotNull(factory.readerMap);
        assertNotNull(factory.writerMap);
        assertEquals(11, factory.readerMap.size());
        assertEquals(11, factory.writerMap.size());

        assertEquals(AssignmentServiceImpl.class, factory.readerMap.get(AssignmentReaderService.class));
        assertEquals(DropboxFolderServiceImpl.class, factory.readerMap.get(DropboxFolderReaderService.class));
        assertEquals(CourseServiceImpl.class, factory.readerMap.get(CourseReaderService.class));
        assertEquals(GradeObjectServiceImpl.class, factory.readerMap.get(GradeObjectReaderService.class));

        assertEquals(AssignmentServiceImpl.class, factory.writerMap.get(AssignmentWriterService.class));
        assertEquals(DropboxFolderServiceImpl.class, factory.writerMap.get(DropboxFolderWriterService.class));
        assertEquals(CourseServiceImpl.class, factory.writerMap.get(CourseWriterService.class));
        assertEquals(GradeObjectServiceImpl.class, factory.writerMap.get(GradeObjectWriterService.class));
    }

    @Test
    void testFourArgConstructor_populatesReaderAndWriterMaps() {
        BrightspaceApiFactory factory = new BrightspaceApiFactory(BASE_URL, 10, 10, apiVersion);

        assertNotNull(factory.readerMap);
        assertNotNull(factory.writerMap);
        assertEquals(11, factory.readerMap.size());
        assertEquals(11, factory.writerMap.size());

        assertEquals(AssignmentServiceImpl.class, factory.readerMap.get(AssignmentReaderService.class));
        assertEquals(DropboxFolderServiceImpl.class, factory.writerMap.get(DropboxFolderWriterService.class));
    }

    /* ===================================== getReader ===================================== */

    @Test
    void testGetReader_twoArg_returnsConcreteInstancePerType() {
        BrightspaceApiFactory factory = new BrightspaceApiFactory(BASE_URL, 10, 10, apiVersion);

        AssignmentReaderService assignmentReader = factory.getReader(AssignmentReaderService.class, oauthToken);
        DropboxFolderReaderService dropboxFolderReader = factory.getReader(DropboxFolderReaderService.class, oauthToken);
        CourseReaderService courseReader = factory.getReader(CourseReaderService.class, oauthToken);

        assertInstanceOf(AssignmentServiceImpl.class, assignmentReader);
        assertInstanceOf(DropboxFolderServiceImpl.class, dropboxFolderReader);
        assertInstanceOf(CourseServiceImpl.class, courseReader);

        assertEquals(BASE_URL, ReflectionTestUtils.getField(assignmentReader, "baseUrl"));
        assertEquals(oauthToken, ReflectionTestUtils.getField(assignmentReader, "oauthToken"));
        assertEquals(apiVersion, ReflectionTestUtils.getField(assignmentReader, "apiVersion"));
        // 2-arg getReader delegates to the 3-arg overload with a null pagination page size
        assertEquals(null, ReflectionTestUtils.getField(assignmentReader, "paginationPageSize"));
    }

    @Test
    void testGetReader_threeArg_appliesExplicitPaginationPageSize() {
        BrightspaceApiFactory factory = new BrightspaceApiFactory(BASE_URL, 10, 10, apiVersion);

        GradeObjectReaderService gradeObjectReader = factory.getReader(GradeObjectReaderService.class, oauthToken, 50);

        assertInstanceOf(GradeObjectServiceImpl.class, gradeObjectReader);
        assertEquals(50, ReflectionTestUtils.getField(gradeObjectReader, "paginationPageSize"));
    }

    @Test
    void testGetReader_unmappedType_throwsUnsupportedOperationException() {
        BrightspaceApiFactory factory = new BrightspaceApiFactory(BASE_URL, apiVersion);

        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> factory.getReader(UnmappedReaderService.class, oauthToken)
        );

        assertTrue(exception.getMessage().contains(UnmappedReaderService.class.getName()));
    }

    /* ===================================== getWriter ===================================== */

    @Test
    void testGetWriter_twoArg_defaultsSerializeNullsToFalse() {
        BrightspaceApiFactory factory = new BrightspaceApiFactory(BASE_URL, 10, 10, apiVersion);

        AssignmentWriterService assignmentWriter = factory.getWriter(AssignmentWriterService.class, oauthToken);
        DropboxFolderWriterService dropboxFolderWriter = factory.getWriter(DropboxFolderWriterService.class, oauthToken);
        CourseWriterService courseWriter = factory.getWriter(CourseWriterService.class, oauthToken);

        assertInstanceOf(AssignmentServiceImpl.class, assignmentWriter);
        assertInstanceOf(DropboxFolderServiceImpl.class, dropboxFolderWriter);
        assertInstanceOf(CourseServiceImpl.class, courseWriter);

        assertEquals(BASE_URL, ReflectionTestUtils.getField(assignmentWriter, "baseUrl"));
        assertEquals(oauthToken, ReflectionTestUtils.getField(assignmentWriter, "oauthToken"));
        assertFalse((Boolean) ReflectionTestUtils.getField(assignmentWriter, "serializeNulls"));
    }

    @Test
    void testGetWriter_threeArg_serializeNullsTrue_isPropagated() {
        BrightspaceApiFactory factory = new BrightspaceApiFactory(BASE_URL, 10, 10, apiVersion);

        GradeObjectWriterService gradeObjectWriter = factory.getWriter(GradeObjectWriterService.class, oauthToken, true);

        assertInstanceOf(GradeObjectServiceImpl.class, gradeObjectWriter);
        assertTrue((Boolean) ReflectionTestUtils.getField(gradeObjectWriter, "serializeNulls"));
    }

    @Test
    void testGetWriter_threeArg_serializeNullsFalse_isPropagated() {
        BrightspaceApiFactory factory = new BrightspaceApiFactory(BASE_URL, 10, 10, apiVersion);

        GradeObjectWriterService gradeObjectWriter = factory.getWriter(GradeObjectWriterService.class, oauthToken, false);

        assertInstanceOf(GradeObjectServiceImpl.class, gradeObjectWriter);
        assertFalse((Boolean) ReflectionTestUtils.getField(gradeObjectWriter, "serializeNulls"));
    }

    @Test
    void testGetWriter_unmappedType_throwsUnsupportedOperationException() {
        BrightspaceApiFactory factory = new BrightspaceApiFactory(BASE_URL, apiVersion);

        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> factory.getWriter(UnmappedWriterService.class, oauthToken)
        );

        assertTrue(exception.getMessage().contains(UnmappedWriterService.class.getName()));
    }

    /* ==================================== test-only fixtures ==================================== */

    // deliberately absent from BrightspaceApiFactory#setupClassMap, to exercise the unmapped-type branch
    private interface UnmappedReaderService extends BrightspaceReaderService<Object, UnmappedReaderService> { }

    private interface UnmappedWriterService extends BrightspaceWriterService<Object, UnmappedWriterService> { }

}
