package com.opiagile.supportai.version;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    private final String appName;
    private final String version;
    private final String environment;
    private final Clock clock;

    public VersionController(
            @Value("${app.name}") String appName,
            @Value("${app.version}") String version,
            @Value("${app.environment}") String environment,
            Clock clock) {
        this.appName = appName;
        this.version = version;
        this.environment = environment;
        this.clock = clock;
    }

    @GetMapping
    public VersionResponse version() {
        return new VersionResponse(
                appName,
                version,
                environment,
                Runtime.version().toString(),
                OffsetDateTime.now(clock));
    }
}
