package edu.iu.terracotta.connectors.brightspace.io.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.brightspace.dao.model.extended.CourseExtended;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceMessengerService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.CourseReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

public class CourseServiceImplTest {

    private static final String BASE_URL = "https://brightspace.example.com";

    @Mock private RestClient restClient;
    @Mock private OauthToken oauthToken;
    @Mock private BrightspaceMessengerService brightspaceMessenger;
    @Mock private ResponseParserService responseParser;

    private ApiVersion apiVersion;
    private CourseServiceImpl courseService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        apiVersion = ApiVersion.builder().le("1.43").build();

        courseService = new CourseServiceImpl(BASE_URL, apiVersion, oauthToken, restClient, 5000, 5000, 50, false);

        courseService.brightspaceMessenger = brightspaceMessenger;
        courseService.responseParser = responseParser;
    }

    @Test
    public void testListCoursesForUserAlwaysReturnsEmptyListRegardlessOfInput() throws IOException {
        // NOTE: listCoursesForUser() is a stub - the real implementation is commented out in
        // CourseServiceImpl and this method unconditionally returns List.of(), never touching the
        // userId argument, the OAuth token, or the REST client. Documented here as a known gap
        // rather than "fixed" by this test.
        assertTrue(courseService.listCoursesForUser(0L).isEmpty());
        assertTrue(courseService.listCoursesForUser(1L).isEmpty());
        assertTrue(courseService.listCoursesForUser(-1L).isEmpty());
        assertTrue(courseService.listCoursesForUser(Long.MAX_VALUE).isEmpty());
    }

    @Test
    public void testObjectTypeReturnsCourseExtended() {
        assertEquals(CourseExtended.class, courseService.objectType());
    }

    @Test
    public void testListTypeReturnsParameterizedListOfCourseExtended() {
        ParameterizedType type = (ParameterizedType) courseService.listType().getType();

        assertEquals(List.class, type.getRawType());
        assertEquals(CourseExtended.class, type.getActualTypeArguments()[0]);
    }

    @Test
    public void testConstructorWiresBaseServiceFields() {
        assertEquals(BASE_URL, courseService.baseUrl);
        assertSame(apiVersion, courseService.apiVersion);
        assertSame(oauthToken, courseService.oauthToken);
        assertEquals(50, courseService.paginationPageSize);
        assertFalse(courseService.serializeNulls);
    }

    @Test
    public void testConstructorTreatsNullSerializeNullsAsFalse() {
        CourseServiceImpl service = new CourseServiceImpl(BASE_URL, apiVersion, oauthToken, restClient, 5000, 5000, 50, null);

        assertFalse(service.serializeNulls);
    }

    @Test
    public void testWithCallbackSetsFieldAndReturnsSameInstanceForChaining() {
        java.util.function.Consumer<List<CourseExtended>> callback = list -> { };

        CourseReaderService result = courseService.withCallback(callback);

        assertSame(courseService, result);
        assertSame(callback, courseService.responseCallback);
    }

    @Test
    public void testInheritedGetReturnsParsedObjectOnSuccess() throws IOException {
        Response response = Response.builder().responseCode(200).content("{}").build();
        CourseExtended course = CourseExtended.builder().build();

        when(brightspaceMessenger.getSingleResponse(eq(oauthToken), any())).thenReturn(response);
        when(responseParser.parseToObject(CourseExtended.class, response)).thenReturn(Optional.of(course));

        Optional<CourseExtended> result = courseService.get(BASE_URL + "/some/path");

        assertTrue(result.isPresent());
        assertSame(course, result.get());
    }

    @Test
    public void testInheritedGetThrowsIOExceptionWhenErrorHappened() throws IOException {
        Response errorResponse = Response.builder().responseCode(200).errorHappened(true).build();

        when(brightspaceMessenger.getSingleResponse(eq(oauthToken), any())).thenReturn(errorResponse);

        assertThrows(IOException.class, () -> courseService.get(BASE_URL + "/some/path"));
    }

    @Test
    public void testInheritedGetThrowsIOExceptionWhenResponseCodeNot200() throws IOException {
        Response notFound = Response.builder().responseCode(404).build();

        when(brightspaceMessenger.getSingleResponse(eq(oauthToken), any())).thenReturn(notFound);

        assertThrows(IOException.class, () -> courseService.get(BASE_URL + "/some/path"));
    }

    @Test
    public void testInheritedGetListAggregatesResponsesAcrossPages() throws IOException {
        Response page1 = Response.builder().responseCode(200).content("page1").build();
        Response page2 = Response.builder().responseCode(200).content("page2").build();
        CourseExtended course1 = CourseExtended.builder().build();
        CourseExtended course2 = CourseExtended.builder().build();

        when(brightspaceMessenger.get(eq(oauthToken), any(), isNull())).thenReturn(List.of(page1, page2));
        when(responseParser.<CourseExtended>parseToList(any(), eq(page1))).thenReturn(List.of(course1));
        when(responseParser.<CourseExtended>parseToList(any(), eq(page2))).thenReturn(List.of(course2));

        List<CourseExtended> result = courseService.getList(BASE_URL + "/some/path");

        assertEquals(2, result.size());
        assertTrue(result.contains(course1));
        assertTrue(result.contains(course2));
    }

}
