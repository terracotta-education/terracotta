package edu.iu.terracotta.config;

import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.api.ApiTokenService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiJwtService;
import edu.iu.terracotta.security.app.ApiOAuthProviderProcessingFilter;
import edu.iu.terracotta.security.lti.Lti3OAuthProviderProcessingFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.CorsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;

@Slf4j
@Configuration
@EnableWebSecurity
// enforces the controllers' role annotations (edu.iu.terracotta.security.app.roles)
@EnableMethodSecurity
@RequiredArgsConstructor
@Import(SecurityAutoConfiguration.class)
public class WebSecurityConfig {

    private final ApiJwtService apiJwtService;
    private final ApiTokenService apiDataService;
    private final LtiDataService ltiDataService;
    private final LtiJwtService ltijwtService;

    @Value("${terracotta.admin.user:admin}")
    private String adminUser;

    @Value("${terracotta.admin.password:admin}")
    private String adminPassword;

    private Lti3OAuthProviderProcessingFilter lti3OAuthProviderProcessingFilter;
    private ApiOAuthProviderProcessingFilter apiOAuthProviderProcessingFilter;

    @PostConstruct
    public void init() {
        apiOAuthProviderProcessingFilter = new ApiOAuthProviderProcessingFilter(apiJwtService, apiDataService);
        lti3OAuthProviderProcessingFilter = new Lti3OAuthProviderProcessingFilter(ltiDataService, ltijwtService);
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
        // deepcode ignore DisablesCSRFProtection: /config is an admin JSON API using HTTP Basic, its writes only accept application/json bodies and this chain sends no CORS headers, so a cross-site page can't forge them
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

        // deepcode ignore DisablesCSRFProtection: LTI launches are cross-site form POSTs from the LMS by design, they are authenticated by the signed id_token, the signed state and the single use nonce
        return http
            .authorizeHttpRequests(
                authz -> authz
                    .requestMatchers("/lti3/**")
                    .permitAll()
            )
            .addFilterBefore(
                lti3OAuthProviderProcessingFilter,
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

        // deepcode ignore DisablesCSRFProtection: the API is stateless and authenticates with an Authorization Bearer JWT, which a browser never attaches to a cross-site request
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
                apiOAuthProviderProcessingFilter,
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
        // deepcode ignore DisablesCSRFProtection: these routes take cross-site LMS posts (OIDC login, notices) protected by signed JWTs or signed state, or bearer tokens, and the one session backed write (POST /registration) only replays what a bearer authenticated GET prepared
        return http
            .securityMatcher("/**")
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
                            "/lms/oauth2/**",
                            "/preview/**",
                            "/notice/**"
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
