package edu.iu;

import edu.iu.terracotta.config.WebSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@Import(WebSecurityConfig.class)
@SuppressWarnings({"PMD.UseUtilityClass"})
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class Terracotta {

    public static void main(String[] args) {
        SpringApplication.run(Terracotta.class, args);
    }

}
