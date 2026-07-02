package com.opiagile.supportai.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class DeveloperPortalController {

    @GetMapping("/developers")
    String developers() {
        return "forward:/developers/index.html";
    }

    @GetMapping("/developers/")
    String developerPortal() {
        return "forward:/developers/index.html";
    }

    @GetMapping("/developers/console")
    String developerConsole() {
        return "forward:/developers/api-console/index.html";
    }
}
