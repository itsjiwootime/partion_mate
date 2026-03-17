package com.project.partition_mate.controller;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.LoginRequest;
import com.project.partition_mate.dto.LoginResponse;
import com.project.partition_mate.dto.SignUpRequest;
import com.project.partition_mate.dto.SignUpResponse;
import com.project.partition_mate.security.RefreshTokenCookieFactory;
import com.project.partition_mate.security.RefreshTokenCookieProperties;
import com.project.partition_mate.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;
    private final RefreshTokenCookieProperties refreshTokenCookieProperties;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signup(@RequestBody @Valid SignUpRequest signUpRequest) {

        User user = authService.signUp(signUpRequest);

        SignUpResponse signUpResponse = SignUpResponse.from(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(signUpResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {

        var issuedLoginTokens = authService.login(loginRequest);
        ResponseCookie refreshTokenCookie = refreshTokenCookieFactory.create(
                issuedLoginTokens.refreshToken().tokenValue(),
                issuedLoginTokens.refreshToken().maxAgeSeconds()
        );
        LoginResponse response = new LoginResponse(
                issuedLoginTokens.accessToken(),
                issuedLoginTokens.accessTokenExpiresInMs()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        var issuedLoginTokens = authService.refresh(extractRefreshToken(request));
        ResponseCookie refreshTokenCookie = refreshTokenCookieFactory.create(
                issuedLoginTokens.refreshToken().tokenValue(),
                issuedLoginTokens.refreshToken().maxAgeSeconds()
        );
        LoginResponse response = new LoginResponse(
                issuedLoginTokens.accessToken(),
                issuedLoginTokens.accessTokenExpiresInMs()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(extractRefreshToken(request));

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString())
                .build();
    }

    @GetMapping("/check-username")
    public ResponseEntity<Void> checkUsername(String username) {
        authService.checkUsernameAvailability(username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-email")
    public ResponseEntity<Void> checkEmail(String email) {
        authService.checkEmailAvailability(email);
        return ResponseEntity.ok().build();
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (refreshTokenCookieProperties.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
