package edu.iu.terracotta.config;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Allows for easy access to the application configuration,
 * merges config settings from spring and local application config
 */
@Slf4j
@Component
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ApplicationConfig implements ApplicationContextAware {

    private static final Object contextLock = new Object();
    private static final Object configLock = new Object();

    @Getter
    @Autowired
    private ConfigurableEnvironment environment;

    @Getter private static ApplicationContext context;
    private static ApplicationConfig config;

    @PostConstruct
    public void init() {
        log.info("INIT");
        environment.setActiveProfiles("dev", "test");
        synchronized (configLock) {
            config = this;
        }
        log.info("Config INIT: profiles active: {}.", ArrayUtils.toString(environment.getActiveProfiles()));
    }

    @PreDestroy
    public void shutdown() {
        synchronized (contextLock) {
            context = null;
        }
        synchronized (configLock) {
            config = null;
        }
        log.info("DESTROY");
    }

    // DELEGATED from the spring Environment (easier config access)

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        synchronized (contextLock) {
            context = applicationContext;
        }
    }

    /**
     * @return the current service instance of the config object (only populated after init)
     */
    public static ApplicationConfig getInstance() {
        return config;
    }

}
