package com.aum.hatchery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// We log users in with our own JWT code, so turn off Spring's default in-memory user
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ProjectHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectHubApplication.class, args);
    }
}
