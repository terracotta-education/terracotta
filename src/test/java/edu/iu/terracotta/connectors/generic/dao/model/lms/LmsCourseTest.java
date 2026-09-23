package edu.iu.terracotta.connectors.generic.dao.model.lms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class LmsCourseTest {

    @Test
    public void testBuilderGettersAndSetters() {
        LmsCourse lmsCourse = LmsCourse.builder()
            .type(LmsCourse.class)
            .id("course-1")
            .build();

        assertEquals(LmsCourse.class, lmsCourse.getType());
        assertEquals("course-1", lmsCourse.getId());

        lmsCourse.setType(String.class);
        lmsCourse.setId("course-2");

        assertEquals(String.class, lmsCourse.getType());
        assertEquals("course-2", lmsCourse.getId());
    }

    @Test
    public void testFromReturnsSameInstance() {
        LmsCourse lmsCourse = LmsCourse.builder().id("course-3").build();

        assertSame(lmsCourse, lmsCourse.from());
    }

}
