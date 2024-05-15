package edu.iu.terracotta.config;

import edu.iu.terracotta.security.app.APIOAuthProviderProcessingFilter;
import edu.iu.terracotta.security.lti.LTI3OAuthProviderProcessingFilter;
import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.service.lti.LTIJWTService;
import edu.iu.terracotta.service.app.APIDataService;
import edu.iu.terracotta.service.app.APIJWTService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.CorsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@Configuration
@EnableWebSecurity
@Import(SecurityAutoConfiguration.class)
public class WebSecurityConfig {

    @Autowired private APIJWTService apiJwtService;
    @Autowired private APIDataService apiDataService;
    @Autowired private LTIDataService ltiDataService;
    @Autowired private LTIJWTService ltijwtService;

    @Value("${terracotta.admin.user:admin}")
    private String adminUser;

    @Value("${terracotta.admin.password:admin}")
    private String adminPassword;

    private LTI3OAuthProviderProcessingFilter lti3oAuthProviderProcessingFilter;
    private APIOAuthProviderProcessingFilter apioAuthProviderProcessingFilter;

    @PostConstruct
    public void init() {
        apioAuthProviderProcessingFilter = new APIOAuthProviderProcessingFilter(apiJwtService, apiDataService);
        lti3oAuthProviderProcessingFilter = new LTI3OAuthProviderProcessingFilter(ltiDataService, ltijwtService);
    }

    @Autowired
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public void configureSimpleAuthUsers(AuthenticationManagerBuilder auth) throws Exception {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        if ("admin".equals(adminPassword)) {
            String adminRandomPwd = UUID.randomUUID().toString();
            log.warn(
                "Admin password not specified, please add one to the application properties file and restart the application. Meanwhile, you can use this one (only valid until the next restart): {}",
                adminRandomPwd
            );
            auth
                .inMemoryAuthentication()
                .withUser(adminUser)
                .password(encoder.encode(adminRandomPwd))
                .roles("ADMIN", "USER");

                return;
        }

        auth
            .inMemoryAuthentication()
            .withUser(adminUser)
            .password(encoder.encode(adminPassword))
            .roles("ADMIN", "USER");
    }

    @Bean
    @Order(30) // VERY HIGH
    SecurityFilterChain filterChain2(HttpSecurity http) throws Exception {
        http.securityMatcher("/config/**");
        return http
                .authorizeHttpRequests(
                    authz -> authz
                        .requestMatchers("/config/**")
                        .authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .headers(frameOptions -> frameOptions.disable())
                .build();
    }

    @Bean
    @Order(40) // HIGH
    SecurityFilterChain filterChain4(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/lti3/**");

        return http
            .authorizeHttpRequests(
                authz -> authz
                    .requestMatchers("/lti3/**")
                    .permitAll()
            )
            .addFilterBefore(
                lti3oAuthProviderProcessingFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .csrf(csrf -> csrf.disable())
            .headers(frameOptions -> frameOptions.disable())
            .build();
    }

    @Bean
    @Order(70)
    SecurityFilterChain filterChain5(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**");

        return http
            .authorizeHttpRequests(
                authz ->
                    authz
                        .requestMatchers("/api/**")
                        .permitAll()
            )
            .addFilterBefore(
                new CorsFilter(new CorsConfigurationSourceImpl()),
                BasicAuthenticationFilter.class
            )
            .addFilterBefore(
                apioAuthProviderProcessingFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .csrf(csrf -> csrf.disable())
            .headers(frameOptions -> frameOptions.disable())
            .build();
    }

    @Bean
    @Order(80) // LOWEST
    SecurityFilterChain filterChain6(HttpSecurity http) throws Exception {
        // this ensures security context info (Principal, sec:authorize, etc.) is accessible on all paths
        return http
            .authorizeHttpRequests(
                authz ->
                    authz
                        .requestMatchers(
                            "/oidc/**",
                            "/registration/**",
                            "/jwks/**",
                            "/deeplink/**",
                            "/ags/**",
                            "/files/**",
                            "/lms/oauth2/**"
                        )
                        .permitAll()
                        .anyRequest()
                        .permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .headers(frameOptions -> frameOptions.disable())
            .build();
    }

    @Bean
    @Order(90) // LOWEST
    SecurityFilterChain noAuthFilterChain(HttpSecurity http) throws Exception {
        // this ensures security context info (Principal, sec:authorize, etc.) is accessible on all paths
        return http
            .authorizeHttpRequests(
                authz ->
                    authz
                        .requestMatchers("/**")
                        .permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .headers(frameOptions -> frameOptions.disable())
            .build();
    }

}
