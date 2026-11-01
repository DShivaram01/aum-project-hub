package com.aum.projecthub.demo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoController {

    @GetMapping("/api/demo")
    public Map<String, Object> demoAccounts() {
        return Map.of(
                "password", DemoDataLoader.PASSWORD,
                "accounts", List.of(
                        Map.of("label", "Faculty - Dr. Maya Patel", "email", DemoDataLoader.FACULTY_EMAIL),
                        Map.of("label", "Student - Alex Rivera", "email", DemoDataLoader.STUDENT_EMAIL)));
    }
}
