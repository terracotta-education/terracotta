package edu.iu;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class TerracottaTest {

    @Test
    void testConstructor() {
        assertNotNull(new Terracotta());
    }

    @Test
    void testMainStartsSpringApplicationWithTerracottaClassAndArgs() {
        String[] args = {"--server.port=8080"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            Terracotta.main(args);

            springApplication.verify(() -> SpringApplication.run(eq(Terracotta.class), eq(args)));
        }
    }

    @Test
    void testMainStartsSpringApplicationWithNoArgs() {
        String[] args = {};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            Terracotta.main(args);

            springApplication.verify(() -> SpringApplication.run(eq(Terracotta.class), eq(args)));
        }
    }

}
