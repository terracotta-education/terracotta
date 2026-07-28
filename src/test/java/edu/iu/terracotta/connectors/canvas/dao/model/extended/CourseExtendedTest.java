package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;

public class CourseExtendedTest {

    @Test
    public void testGetIdReturnsNullWhenCourseIsNull() {
        CourseExtended courseExtended = CourseExtended.builder().course(null).build();

        assertNull(courseExtended.getId());
    }

    @Test
    public void testGetIdReturnsNullWhenCourseIdIsNull() {
        CourseExtended courseExtended = CourseExtended.builder().build();

        assertNotNull(courseExtended.getCourse());
        assertNull(courseExtended.getCourse().getId());
        assertNull(courseExtended.getId());
    }

    @Test
    public void testGetIdReturnsStringWhenCourseIdIsSet() {
        CourseExtended courseExtended = CourseExtended.builder().build();
        courseExtended.getCourse().setId(88L);

        assertEquals("88", courseExtended.getId());
    }

    @Test
    public void testSetIdDoesNothingWhenCourseIsNull() {
        CourseExtended courseExtended = CourseExtended.builder().course(null).build();

        // First guard: `course == null` should short-circuit before touching `id`.
        courseExtended.setId("123");

        assertNull(courseExtended.getId());
    }

    @Test
    public void testSetIdDoesNothingWhenIdIsNull() {
        CourseExtended courseExtended = CourseExtended.builder().build();

        // Second, independent guard: `id == null` should short-circuit even though
        // `course` itself is present.
        courseExtended.setId(null);

        assertNull(courseExtended.getCourse().getId());
        assertNull(courseExtended.getId());
    }

    @Test
    public void testSetIdParsesAndSetsWhenCourseAndIdArePresent() {
        CourseExtended courseExtended = CourseExtended.builder().build();

        courseExtended.setId("555");

        assertEquals(Long.valueOf(555L), courseExtended.getCourse().getId());
        assertEquals("555", courseExtended.getId());
    }

    @Test
    public void testFromCopiesIdAndType() {
        CourseExtended courseExtended = CourseExtended.builder().build();
        courseExtended.getCourse().setId(20L);

        LmsCourse lmsCourse = courseExtended.from();

        assertNotNull(lmsCourse);
        assertEquals("20", lmsCourse.getId());
        assertEquals(CourseExtended.class, lmsCourse.getType());
    }

    @Test
    public void testOfReturnsDefaultInstanceWhenLmsCourseIsNull() {
        CourseExtended courseExtended = CourseExtended.of(null);

        assertNotNull(courseExtended);
        assertNull(courseExtended.getId());
    }

    @Test
    public void testOfCopiesIdWhenLmsCourseIsNonNull() {
        LmsCourse lmsCourse = LmsCourse.builder().id("777").build();

        CourseExtended courseExtended = CourseExtended.of(lmsCourse);

        assertEquals("777", courseExtended.getId());
    }

    @Test
    public void testOfWithLmsCourseWithNullIdDoesNotThrowAndYieldsNullId() {
        // Unlike ConversationExtended, CourseExtended#setId(String) guards against a
        // null id, so of(...) with an LmsCourse whose id is null is safe and simply
        // results in a CourseExtended with a null id.
        LmsCourse lmsCourse = LmsCourse.builder().build();

        CourseExtended courseExtended = CourseExtended.of(lmsCourse);

        assertNull(courseExtended.getId());
    }

}
