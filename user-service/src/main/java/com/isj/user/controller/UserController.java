package com.isj.user.controller;

import com.isj.common.dto.ApiResponse;
import com.isj.user.dto.LoginRequest;
import com.isj.user.dto.SignUpRequest;
import com.isj.user.dto.TokenResponse;
import com.isj.user.dto.UserResponse;
import com.isj.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/users/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.ok("User created successfully", userService.signUp(request));
    }

    @PostMapping("/auth/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.ok(userService.getUser(userId));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getUser(userId));
    }
}
