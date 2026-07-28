package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.canvas.service.extended.CourseReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.CourseWriterExtended;
import edu.ksu.canvas.impl.AssignmentImpl;
import edu.ksu.canvas.impl.CourseImpl;
import edu.ksu.canvas.interfaces.AssignmentReader;
import edu.ksu.canvas.interfaces.AssignmentWriter;
import edu.ksu.canvas.interfaces.CourseReader;
import edu.ksu.canvas.interfaces.CourseWriter;
import edu.ksu.canvas.interfaces.RubricReader;
import edu.ksu.canvas.interfaces.RubricWriter;
import edu.ksu.canvas.oauth.OauthToken;

public class CanvasApiFactoryExtendedTest {

    private static final String BASE_URL = "https://canvas.example.com";

    @Mock private OauthToken oauthToken;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSingleArgConstructorPopulatesMaps() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL);

        assertNotNull(factory.readerMap);
        assertNotNull(factory.writerMap);
        assertFalse(factory.readerMap.isEmpty());
        assertFalse(factory.writerMap.isEmpty());
        assertEquals(35, factory.readerMap.size());
        assertEquals(32, factory.writerMap.size());
        assertEquals(CourseImpl.class, factory.readerMap.get(CourseReader.class));
        assertEquals(AssignmentImpl.class, factory.readerMap.get(AssignmentReader.class));
        assertEquals(CourseImpl.class, factory.writerMap.get(CourseWriter.class));
        assertEquals(AssignmentImpl.class, factory.writerMap.get(AssignmentWriter.class));
    }

    @Test
    public void testBatchSizeConstructorPopulatesMaps() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL, 50);

        assertEquals(35, factory.readerMap.size());
        assertEquals(32, factory.writerMap.size());
        assertEquals(CourseImpl.class, factory.readerMap.get(CourseReader.class));
    }

    @Test
    public void testTimeoutsConstructorPopulatesMaps() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL, 1000, 2000);

        assertEquals(35, factory.readerMap.size());
        assertEquals(32, factory.writerMap.size());
        assertEquals(CourseImpl.class, factory.readerMap.get(CourseReader.class));
    }

    @Test
    public void testGetReaderReturnsRealConcreteInstanceForKsuInterface() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL);

        CourseReader reader = factory.getReader(CourseReader.class, oauthToken);

        assertTrue(reader instanceof CourseImpl);
    }

    @Test
    public void testGetReaderReturnsRealConcreteInstanceForExtendedInterface() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL);

        CourseReaderExtended reader = factory.getReader(CourseReaderExtended.class, oauthToken);

        assertTrue(reader instanceof CourseExtendedImpl);
    }

    @Test
    public void testGetReaderWithExplicitPaginationPageSize() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL);

        AssignmentReader reader = factory.getReader(AssignmentReader.class, oauthToken, 25);

        assertTrue(reader instanceof AssignmentImpl);
    }

    @Test
    public void testGetReaderThrowsForUnmappedType() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL);

        assertThrows(UnsupportedOperationException.class, () -> factory.getReader(RubricReader.class, oauthToken));
    }

    @Test
    public void testGetWriterReturnsRealConcreteInstanceForKsuInterface() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL);

        CourseWriter writer = factory.getWriter(CourseWriter.class, oauthToken);

        assertTrue(writer instanceof CourseImpl);
    }

    @Test
    public void testGetWriterReturnsRealConcreteInstanceForExtendedInterface() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL);

        CourseWriterExtended writer = factory.getWriter(CourseWriterExtended.class, oauthToken);

        assertTrue(writer instanceof CourseExtendedImpl);
    }

    @Test
    public void testGetWriterWithSerializeNullsTrue() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL);

        AssignmentWriter writer = factory.getWriter(AssignmentWriter.class, oauthToken, true);

        assertTrue(writer instanceof AssignmentImpl);
    }

    @Test
    public void testGetWriterWithSerializeNullsFalse() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL);

        AssignmentWriter writer = factory.getWriter(AssignmentWriter.class, oauthToken, false);

        assertTrue(writer instanceof AssignmentImpl);
    }

    @Test
    public void testGetWriterThrowsForUnmappedType() {
        CanvasApiFactoryExtended factory = new CanvasApiFactoryExtended(BASE_URL);

        assertThrows(UnsupportedOperationException.class, () -> factory.getWriter(RubricWriter.class, oauthToken));
    }

}
