package com.example.demo.service.security;

import com.example.demo.config.BaseResponse;
import com.example.demo.dto.auth.ChangePasswordRequest;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.LoginResponse;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {
    BaseResponse<LoginResponse> login(LoginRequest req);
    BaseResponse<Void> changePassword(String email, ChangePasswordRequest req);
}
