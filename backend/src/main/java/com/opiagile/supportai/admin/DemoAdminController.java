package com.opiagile.supportai.admin;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opiagile.supportai.common.ErrorResponse;
import com.opiagile.supportai.security.DemoSecurityPolicy;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/demo")
public class DemoAdminController {

    private final DemoAdminService demoAdminService;
    private final DemoSecurityPolicy securityPolicy;

    public DemoAdminController(DemoAdminService demoAdminService, DemoSecurityPolicy securityPolicy) {
        this.demoAdminService = demoAdminService;
        this.securityPolicy = securityPolicy;
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(HttpServletRequest request) {
        if (!securityPolicy.adminResetEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("RESET_DEMO_DESABILITADO", "Reset da demo desabilitado neste ambiente.", OffsetDateTime.now()));
        }
        if (!securityPolicy.adminTokenMatches(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("TOKEN_ADMIN_INVALIDO", "Token administrativo ausente ou inválido.", OffsetDateTime.now()));
        }
        return ResponseEntity.ok(demoAdminService.resetDemoData());
    }
}
