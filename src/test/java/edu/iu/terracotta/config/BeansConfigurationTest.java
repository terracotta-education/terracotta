package edu.iu.terracotta.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

public class BeansConfigurationTest {

    private BeansConfiguration beansConfiguration;

    @BeforeEach
    void beforeEach() {
        beansConfiguration = new BeansConfiguration();
    }

    @Test
    void testJavaMailSender() {
        JavaMailSenderImpl javaMailSender = beansConfiguration.javaMailSender();

        assertNotNull(javaMailSender);
    }

    // waitForTasksToCompleteOnShutdown makes ThreadPoolTaskExecutor a SmartLifecycle participant in
    // Spring's ordered shutdown phase, so in-flight @Async tasks (LMS roster syncs, exports, etc.)
    // finish before the context proceeds to destroy the EntityManagerFactory - without this, those
    // tasks race context shutdown and fail with "EntityManagerFactory is closed".
    @Test
    void testTaskExecutorWaitsForTasksToCompleteOnShutdown() {
        Executor executor = beansConfiguration.taskExecutor(4, 8, 100, 1800);

        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);

        ThreadPoolTaskExecutor threadPoolTaskExecutor = (ThreadPoolTaskExecutor) executor;
        assertEquals(4, threadPoolTaskExecutor.getCorePoolSize());
        assertEquals(8, threadPoolTaskExecutor.getMaxPoolSize());
        assertEquals(100, threadPoolTaskExecutor.getQueueCapacity());
        assertTrue((Boolean) ReflectionTestUtils.getField(threadPoolTaskExecutor, "waitForTasksToCompleteOnShutdown"));
        assertEquals(1800_000L, (long) ReflectionTestUtils.getField(threadPoolTaskExecutor, "awaitTerminationMillis"));
    }

}
