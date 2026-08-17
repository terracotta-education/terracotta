package edu.iu.terracotta.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BeansConfiguration {

    @Bean
    public JavaMailSenderImpl javaMailSender() {
        return new JavaMailSenderImpl();
    }

    /**
     * Named "taskExecutor" so Spring's @Async infrastructure picks it up automatically instead of
     * falling back to a plain SimpleAsyncTaskExecutor, which isn't a managed bean and has no
     * shutdown coordination - letting in-flight @Async tasks (LMS roster syncs, exports, etc.)
     * race a mid-shutdown EntityManagerFactory close and fail with "EntityManagerFactory is
     * closed". waitForTasksToCompleteOnShutdown makes this executor participate in Spring's
     * ordered lifecycle shutdown phase, so it waits for running tasks before the context proceeds
     * to destroy other beans.
     */
    @Bean
    public Executor taskExecutor(
        @Value("${app.async.core-pool-size:4}") int corePoolSize,
        @Value("${app.async.max-pool-size:8}") int maxPoolSize,
        @Value("${app.async.queue-capacity:100}") int queueCapacity,
        @Value("${app.async.await-termination-seconds:1800}") int awaitTerminationSeconds
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("terracotta-async-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();

        return executor;
    }

}
