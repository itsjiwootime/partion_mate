package com.project.partition_mate.controller;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.LoginRequest;
import com.project.partition_mate.dto.LoginResponse;
import com.project.partition_mate.dto.SignUpRequest;
import com.project.partition_mate.dto.SignUpResponse;
import com.project.partition_mate.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signup(@RequestBody @Valid SignUpRequest signUpRequest) {

        User user = authService.signUp(signUpRequest);

        SignUpResponse signUpResponse = SignUpResponse.from(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(signUpResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {

        LoginResponse response = authService.login(loginRequest);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-username")
    public ResponseEntity<Void> checkUsername(String username) {
        authService.checkUsernameAvailability(username);
        return ResponseEntity.ok().build();
    }
}
