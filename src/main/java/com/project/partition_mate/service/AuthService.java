package com.project.partition_mate.service;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.LoginRequest;
import com.project.partition_mate.dto.SignUpRequest;
import com.project.partition_mate.exception.CustomAuthException;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.security.IssuedLoginTokens;
import com.project.partition_mate.security.JwtTokenProvider;
import com.project.partition_mate.security.RefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String LOGIN_FAILURE_DUMMY_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi2x6YQ0G6lQ4c6Ih0b7D8Y1koXSP9m";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public User signUp(SignUpRequest signUpRequest) {
        checkEmailAvailability(signUpRequest.getEmail());
        checkUsernameAvailability(signUpRequest.getUsername());

        String encodedPassword = passwordEncoder.encode(signUpRequest.getPassword());

        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encodedPassword,
                signUpRequest.getAddress(),
                signUpRequest.getLatitude(),
                signUpRequest.getLongitude()
        );

        userRepository.save(user);


        return user;
    }

    @Transactional
    public IssuedLoginTokens login(LoginRequest loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);
        String passwordHash = user != null ? user.getPassword() : LOGIN_FAILURE_DUMMY_PASSWORD_HASH;
        boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), passwordHash);

        if (user == null || !passwordMatches) {
            throw CustomAuthException.LOGIN_FAILED;
        }

        String jwtToken = jwtTokenProvider.createToken(user.getId());
        var refreshToken = refreshTokenService.issue(user);

        return new IssuedLoginTokens(
                jwtToken,
                jwtTokenProvider.getAccessTokenExpirationTime(),
                refreshToken
        );
    }

    @Transactional
    public IssuedLoginTokens refresh(String rawRefreshToken) {
        Long userId = refreshTokenService.findUserId(rawRefreshToken);
        var rotatedRefreshToken = refreshTokenService.rotate(rawRefreshToken);
        String jwtToken = jwtTokenProvider.createToken(userId);

        return new IssuedLoginTokens(
                jwtToken,
                jwtTokenProvider.getAccessTokenExpirationTime(),
                rotatedRefreshToken
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    public void checkUsernameAvailability(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new CustomAuthException("Username already exists", org.springframework.http.HttpStatus.CONFLICT);
        }
    }

    public void checkEmailAvailability(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw CustomAuthException.EMAIL_DUPLICATE;
        }
    }
}
