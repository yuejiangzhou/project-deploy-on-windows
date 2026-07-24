package com.company.deploy.controller;

import com.company.deploy.common.Result;
import com.company.deploy.dto.UserCreateRequest;
import com.company.deploy.dto.UserUpdateRequest;
import com.company.deploy.entity.UserEntity;
import com.company.deploy.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<UserEntity>> listUsers() {
        return Result.success(userService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserEntity> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserEntity user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getDisplayName(),
                request.getRole());
        // 返回时清除密码字段
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserEntity> updateUser(@PathVariable Long id,
                                         @Valid @RequestBody UserUpdateRequest request) {
        UserEntity user = userService.updateUser(id, request.getDisplayName(), request.getRole(), request.getStatus());
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/{id}/password")
    public Result<Void> updatePassword(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error(400, "原密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error(400, "新密码不能为空");
        }
        if (newPassword.length() < 6) {
            return Result.error(400, "新密码长度不能少于6位");
        }
        try {
            userService.updatePassword(id, oldPassword, newPassword);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
