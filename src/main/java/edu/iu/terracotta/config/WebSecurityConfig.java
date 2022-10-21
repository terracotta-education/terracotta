/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.terracotta.config;

import edu.iu.terracotta.security.app.APIOAuthProviderProcessingFilter;
import edu.iu.terracotta.security.lti.LTI3OAuthProviderProcessingFilter;
import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.service.lti.LTIJWTService;
import edu.iu.terracotta.service.app.APIDataService;
import edu.iu.terracotta.service.app.APIJWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@Import(SecurityAutoConfiguration.class)
public class WebSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Value("${terracotta.admin.user:admin}")
    private String adminUser;

    @Value("${terracotta.admin.password:admin}")
    private String adminPassword;

    @Value("${api.oauth.provider.processing.filter.enabled:true}")
    private boolean apioAuthProviderProcessingFilterEnabled;

    @Autowired
    private APIJWTService apiJwtService;

    @Autowired
    private APIDataService apiDataService;

    @Autowired
    private LTIDataService ltiDataService;

    @Autowired
    private LTIJWTService ltijwtService;

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
            logger.warn("Admin password not specified, please add one to the application properties file and restart the application. Meanwhile, you can use this one (only valid until the next restart): {}",
                adminRandomPwd);
            auth.inMemoryAuthentication()
                    .withUser(adminUser).password(encoder.encode(adminRandomPwd)).roles("ADMIN", "USER");
        } else {
            auth.inMemoryAuthentication()
                    .withUser(adminUser).password(encoder.encode(adminPassword)).roles("ADMIN", "USER");
        }
    }

    @Bean
    @Order(10) // VERY HIGH
    public SecurityFilterChain openEndpointsFilterChain(HttpSecurity http) throws Exception {
        // this is open
        http.requestMatchers()
            .antMatchers("/oidc/**")
            .antMatchers("/registration/**")
            .antMatchers("/jwks/**")
            .antMatchers("/files/**")
                .and()
            .authorizeRequests().anyRequest().permitAll().and().csrf().disable().headers().frameOptions().disable();

            return http.build();
    }

    @Bean
    @Order(30) // VERY HIGH
    public SecurityFilterChain configFilterChain(HttpSecurity http) throws Exception {
        http.antMatcher("/config/**").authorizeRequests().anyRequest().authenticated().and().httpBasic().and().csrf().disable().headers().frameOptions().disable();

        return http.build();
    }

    @Bean
    @Order(40) // HIGH
    public SecurityFilterChain lti3OAuthProviderProcessingFilterChain(HttpSecurity http) throws Exception {
        http.requestMatchers().antMatchers("/lti3/**").and()
                .addFilterBefore(lti3oAuthProviderProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests().anyRequest().permitAll().and().csrf().disable().headers().frameOptions().disable();

        return http.build();
    }

    @Bean
    @Order(50) // HIGH
    public SecurityFilterChain apiSecurityConfigurerFilterChain(HttpSecurity http) throws Exception {
        HttpSecurity httpSecurity = http.requestMatchers()
                .antMatchers("/api/**")
                .and()
                .addFilterBefore(new CorsFilter(new CorsConfigurationSourceImpl()),
                        BasicAuthenticationFilter.class);

        if (apioAuthProviderProcessingFilterEnabled) {
            logger.info("Adding APIOAuthProviderProcessingFilter to all /api/ requests");
            httpSecurity = httpSecurity
                    .addFilterBefore(apioAuthProviderProcessingFilter,
                            UsernamePasswordAuthenticationFilter.class);
        }

        httpSecurity
                .authorizeRequests()
                .anyRequest()
                .permitAll()
                .and()
                .csrf()
                .disable()
                .headers()
                .frameOptions()
                .disable();

                return httpSecurity.build();
    }

    @Bean
    @Order(80) // LOWEST
    public SecurityFilterChain noAuthFilterChain(HttpSecurity http) throws Exception {
        // this ensures security context info (Principal, sec:authorize, etc.) is accessible on all paths
        http.antMatcher("/**").authorizeRequests().anyRequest().permitAll().and().headers().frameOptions().disable();

        return http.build();
    }

}
