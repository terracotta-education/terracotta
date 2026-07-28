package edu.iu.terracotta.connectors.brightspace.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;

public class CourseExtendedTest {

    @Test
    public void testOfWithNullLmsCourseReturnsEmptyCourseExtended() {
        CourseExtended courseExtended = CourseExtended.of(null);

        assertNotNull(courseExtended);
        assertNull(courseExtended.getId());
    }

    @Test
    public void testOfWithLmsCourseCopiesId() {
        LmsCourse lmsCourse = LmsCourse.builder()
            .id("course-123")
            .build();

        CourseExtended courseExtended = CourseExtended.of(lmsCourse);

        assertNotNull(courseExtended);
        assertEquals("course-123", courseExtended.getId());
    }

    @Test
    public void testGetIdDelegatesToNestedCourseIdentifier() {
        CourseExtended courseExtended = CourseExtended.builder().build();
        courseExtended.getCourse().setIdentifier("identifier-1");

        assertEquals("identifier-1", courseExtended.getId());
    }

    @Test
    public void testSetIdDelegatesToNestedCourseIdentifier() {
        CourseExtended courseExtended = CourseExtended.builder().build();
        courseExtended.setId("identifier-2");

        assertEquals("identifier-2", courseExtended.getCourse().getIdentifier());
    }

    @Test
    public void testFromMapsIdAndType() {
        CourseExtended courseExtended = CourseExtended.builder().build();
        courseExtended.setId("course-456");

        LmsCourse lmsCourse = courseExtended.from();

        assertNotNull(lmsCourse);
        assertEquals("course-456", lmsCourse.getId());
        assertEquals(CourseExtended.class, lmsCourse.getType());
    }

}
