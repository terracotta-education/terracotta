package edu.iu.terracotta.config;

import edu.iu.terracotta.security.app.APIOAuthProviderProcessingFilter;
import edu.iu.terracotta.security.lti.LTI3OAuthProviderProcessingFilter;
import edu.iu.terracotta.security.lti.LTI3OAuthProviderProcessingFilterStateNonceChecked;
import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.service.lti.LTIJWTService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import edu.iu.terracotta.service.app.APIDataService;
import edu.iu.terracotta.service.app.APIJWTService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.CorsFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import java.util.UUID;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@Configuration
@EnableWebSecurity
@SuppressWarnings({"PMD.GuardLogStatement"})
public class WebSecurityConfig {

    @Value("${terracotta.admin.user:admin}")
    String adminUser;

    @Value("${terracotta.admin.password:admin}")
    String adminPassword;

    @Autowired private LTIDataService ltiDataService;
    @Autowired private LTIJWTService ltijwtService;
    @Autowired private APIJWTService apiJwtService;
    @Autowired private APIDataService apiDataService;

    private LTI3OAuthProviderProcessingFilterStateNonceChecked lti3OAuthProviderProcessingFilterStateNonceChecked;
    private LTI3OAuthProviderProcessingFilter lti3oAuthProviderProcessingFilter;
    private APIOAuthProviderProcessingFilter apioAuthProviderProcessingFilter;

    @PostConstruct
    public void init() {
        lti3OAuthProviderProcessingFilterStateNonceChecked = new LTI3OAuthProviderProcessingFilterStateNonceChecked(ltiDataService, ltijwtService);
        lti3oAuthProviderProcessingFilter = new LTI3OAuthProviderProcessingFilter(ltiDataService, ltijwtService);
        apioAuthProviderProcessingFilter = new APIOAuthProviderProcessingFilter(apiJwtService, apiDataService);
    }

    @Autowired
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public void configureSimpleAuthUsers(AuthenticationManagerBuilder auth) throws Exception {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        if (!StringUtils.equals(adminPassword, "admin")) {
            auth
                .inMemoryAuthentication()
                .withUser(adminUser)
                .password(encoder.encode(adminPassword))
                .roles(
                    "ADMIN",
                    "USER"
                );

            return;
        }

        String adminRandomPwd = UUID.randomUUID().toString();
        log.warn("Admin password not specified, please add one to the application properties file and restart the application." +
                " Meanwhile, you can use this one (only valid until the next restart): {}", adminRandomPwd);
        auth
            .inMemoryAuthentication()
            .withUser(adminUser)
            .password(encoder.encode(adminRandomPwd))
            .roles(
                "ADMIN",
                "USER"
            );
    }

    @Bean
    @Order(30) // VERY HIGH
    public SecurityFilterChain filterChain2(HttpSecurity http) throws Exception {
        http.securityMatcher("/config/**");

        return http
            .authorizeHttpRequests(
                authz ->
                    authz
                        .requestMatchers("/config/**")
                        .authenticated()
            )
            .httpBasic(withDefaults())
            .csrf(csrf -> csrf.disable())
            .headers(frameOptions -> frameOptions.disable())
            .build();
    }

    @Bean
    @Order(35) // HIGH
    public SecurityFilterChain filterChain3(HttpSecurity http) throws Exception {
        http.securityMatcher("/lti3/stateNonceChecked");

        return http
            .authorizeHttpRequests(
                authz ->
                    authz
                        .requestMatchers("/lti3/stateNonceChecked")
                        .permitAll()
            )
            .addFilterAfter(
                lti3OAuthProviderProcessingFilterStateNonceChecked,
                UsernamePasswordAuthenticationFilter.class
            )
            .csrf(csrf -> csrf.disable())
            .headers(frameOptions -> frameOptions.disable())
            .build();
    }

    @Bean
    @Order(40) // HIGH
    public SecurityFilterChain filterChain4(HttpSecurity http) throws Exception {
        http.securityMatcher("/lti3/**");

        return http
            .authorizeHttpRequests(
                authz ->
                    authz
                        .requestMatchers("/lti3/**").permitAll()
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
    public SecurityFilterChain filterChain5(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**");

        return http
            .authorizeHttpRequests(
                authz ->
                    authz
                        .requestMatchers("/api/**")
                        .permitAll()
            )
            .addFilterBefore(
                new CorsFilter(
                    new CorsConfigurationSourceImpl()
                ),
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
    public SecurityFilterChain filterChain6(HttpSecurity http) throws Exception {
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
                            "/ags/**"
                        )
                        .permitAll()
                        .anyRequest()
                        .permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .headers(frameOptions -> frameOptions.disable())
            .build();
    }

}
