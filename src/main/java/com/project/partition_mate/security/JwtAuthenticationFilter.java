package com.project.partition_mate.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return path.startsWith("/api/auth/");

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 요청 헤더에서 JWT 토큰을 추출 (위에 만든 resolveToken 사용)
        String token = resolveToken(request);

        // 2. 토큰이 유효한지 확인
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰에서 유저 정보를 꺼내 인증 객체를 만듭니다.
            Authentication auth = jwtTokenProvider.getAuthentication(token);

            // 4. 시큐리티 세션에 인증 객체를 저장 ("통과 도장 쾅!")
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // 5. 다음 필터로 넘겨줍니다. (이걸 안하면 요청이 여기서 멈춰요)
        filterChain.doFilter(request, response);



    }

    private String resolveToken(HttpServletRequest requesst) {
        String bearerToken = requesst.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
