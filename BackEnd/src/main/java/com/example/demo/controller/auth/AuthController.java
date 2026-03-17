package com.example.demo.controller.auth;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.auth.ChangePasswordRequest;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.LoginResponse;
import com.example.demo.service.security.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/change-password")
    public BaseResponse<Void> changePassword(Authentication auth,
                                             @Valid @RequestBody ChangePasswordRequest req) {
        // auth.getName() = email (subject del token)
        return authService.changePassword(auth.getName(), req);
    }
}
