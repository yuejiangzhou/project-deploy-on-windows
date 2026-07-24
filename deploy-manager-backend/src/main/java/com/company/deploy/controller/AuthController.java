package com.company.deploy.controller;

import com.company.deploy.common.Result;
import com.company.deploy.dto.LoginRequest;
import com.company.deploy.dto.LoginResponse;
import com.company.deploy.entity.UserEntity;
import com.company.deploy.security.JwtTokenProvider;
import com.company.deploy.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserEntity user = userService.findByUsername(request.getUsername());
        userService.updateLastLogin(user.getId());

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .build();

        return Result.success(response);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态，客户端清除 token 即可
        SecurityContextHolder.clearContext();
        return Result.success();
    }

    @GetMapping("/me")
    public Result<LoginResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.error(401, "未登录");
        }

        String username = authentication.getName();
        UserEntity user = userService.findByUsername(username);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        LoginResponse response = LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .build();

        return Result.success(response);
    }
}
