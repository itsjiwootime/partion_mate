package com.project.partition_mate.service;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.LoginRequest;
import com.project.partition_mate.dto.LoginResponse;
import com.project.partition_mate.dto.SignUpRequest;
import com.project.partition_mate.exception.CustomAuthException;
import com.project.partition_mate.repository.UserRepository;
import com.project.partition_mate.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public User signUp(SignUpRequest signUpRequest) {
        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            throw CustomAuthException.EMAIL_DUPLICATE;
        }
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
    public LoginResponse login(LoginRequest loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> CustomAuthException.USER_NOT_FOUND);


        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw CustomAuthException.INVALID_PASSWORD;
        }

        String jwtToken = jwtTokenProvider.createToken(user.getId());

        return new LoginResponse(jwtToken);
    }

    public void checkUsernameAvailability(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new CustomAuthException("Username already exists", org.springframework.http.HttpStatus.CONFLICT);
        }
    }
}
