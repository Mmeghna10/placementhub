package com.placementhub.backend;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Legacy "is it up?" page (requires login). Hidden from the API docs; proper health and
// readiness endpoints come with Spring Boot Actuator in Milestone 6.
@Hidden
@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "Placement Hub Backend is Live!";
    }
}
