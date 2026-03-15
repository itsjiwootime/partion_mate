package com.project.partition_mate.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. REST API이므로 기본 설정들 해제
                .httpBasic(basic -> basic.disable())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())

                // CORS
                .cors(cors -> {})

                // 2. JWT를 쓰므로 세션을 만들지도, 사용하지도 않음 (핵심!)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. URL별 접근 권한 관리
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()    // 회원가입, 로그인은 무조건 허용
                        .requestMatchers(HttpMethod.GET,"/api/stores/**").permitAll() // 지점/파티 리스트 공개
                        .requestMatchers(HttpMethod.GET, "/party/**").permitAll() // 파티 목록/상세 공개
                        .anyRequest().authenticated()                   // 나머지는 모두 인증 필요
                )

                // 4. 💡 가장 중요한 필터 배치!
                // 기본 로그인 필터(UsernamePasswordAuthenticationFilter)가 작동하기 전에
                // 우리가 만든 JWT 필터를 먼저 실행하라는 뜻입니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 비밀번호 암호화 도구 (회원가입/로그인 시 사용)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
