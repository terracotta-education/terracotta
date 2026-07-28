package edu.iu.terracotta.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link ApplicationConfig#context} and {@link ApplicationConfig#getInstance()}'s backing
 * {@code config} field are static and shared across the entire JVM running the test suite, so
 * they can leak state between test methods (and even between test classes, if any other test
 * were to touch this class in the same JVM). {@code shutdown()} is called in both
 * {@code @BeforeEach} and {@code @AfterEach} to force a known-clean slate regardless of
 * execution order.
 */
@ExtendWith(MockitoExtension.class)
public class ApplicationConfigTest {

    @Mock private ConfigurableEnvironment environment;
    @Mock private ApplicationContext applicationContext;

    private ApplicationConfig applicationConfig;

    @BeforeEach
    void beforeEach() {
        applicationConfig = new ApplicationConfig(environment);
        applicationConfig.shutdown();
    }

    @AfterEach
    void afterEach() {
        applicationConfig.shutdown();
    }

    @Test
    void testGetInstanceNullWhenNotInitialized() {
        assertNull(ApplicationConfig.getInstance());
    }

    @Test
    void testGetContextNullWhenNotInitialized() {
        assertNull(ApplicationConfig.getContext());
    }

    @Test
    void testInit() {
        applicationConfig.init();

        verify(environment).setActiveProfiles("dev", "test");
        assertSame(applicationConfig, ApplicationConfig.getInstance());
    }

    @Test
    void testSetApplicationContext() {
        applicationConfig.setApplicationContext(applicationContext);

        assertSame(applicationContext, ApplicationConfig.getContext());
    }

    @Test
    void testShutdownNullsConfigAndContext() {
        applicationConfig.init();
        applicationConfig.setApplicationContext(applicationContext);

        assertSame(applicationConfig, ApplicationConfig.getInstance());
        assertSame(applicationContext, ApplicationConfig.getContext());

        applicationConfig.shutdown();

        assertNull(ApplicationConfig.getInstance());
        assertNull(ApplicationConfig.getContext());
    }

}
