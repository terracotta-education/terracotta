package edu.iu.terracotta.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(
                "/webjars/**",
                "/img/**",
                "/css/**",
                "/js/**")
                .addResourceLocations(
                        "classpath:/META-INF/resources/webjars/",
                        "classpath:/static/img/",
                        "classpath:/static/css/",
                        "classpath:/static/js/");

        // Vite names every built JS/CSS file after a hash of its content, so a changed file always
        // gets a new URL and the old one never needs revalidating
        registry.addResourceHandler("/app/assets/**")
                .addResourceLocations("classpath:/static/app/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

        // the integration resize scripts keep fixed names because partner pages (e.g. Qualtrics)
        // load them by URL; cache them briefly so a change reaches those pages within the hour
        registry.addResourceHandler("/app/js/**")
                .addResourceLocations("classpath:/static/app/js/")
                .setCacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic());

        // the HTML entry pages name the current build's hashed assets, and a deploy deletes the
        // previous build's, so browsers must revalidate them on every load (cheap: Last-Modified)
        registry.addResourceHandler("/app/**")
                .addResourceLocations("classpath:/static/app/")
                .setCacheControl(CacheControl.noCache());
    }

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(true);
        filter.setIncludeClientInfo(true);

        return filter;
    }

}
