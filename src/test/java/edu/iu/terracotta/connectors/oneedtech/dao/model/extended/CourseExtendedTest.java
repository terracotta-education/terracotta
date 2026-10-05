package edu.iu.terracotta.connectors.oneedtech.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import edu.iu.terracotta.connectors.oneedtech.dao.model.lms.Course;

public class CourseExtendedTest {

    @Test
    public void testDefaultCourseIsUsedWhenNoneProvided() {
        CourseExtended courseExtended = CourseExtended.builder().build();

        assertNotNull(courseExtended.getCourse());
    }

    @Test
    public void testBuilderAcceptsExplicitCourse() {
        Course course = Course.builder().build();

        CourseExtended courseExtended = CourseExtended.builder()
            .course(course)
            .build();

        assertSame(course, courseExtended.getCourse());
    }

    @Test
    public void testGetIdAlwaysReturnsOne() {
        CourseExtended courseExtended = CourseExtended.builder().build();

        assertEquals("1", courseExtended.getId());
    }

    @Test
    public void testFromSetsTypeToCourseAndReturnsSameInstance() {
        CourseExtended courseExtended = CourseExtended.builder().build();

        LmsCourse converted = courseExtended.from();

        assertSame(courseExtended, converted);
        assertEquals(Course.class, converted.getType());
        assertEquals("1", converted.getId());
    }

}
