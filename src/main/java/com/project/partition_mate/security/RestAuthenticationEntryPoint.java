package com.project.partition_mate.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.partition_mate.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String code = (String) request.getAttribute(SecurityErrorAttributes.AUTH_ERROR_CODE);
        String message = (String) request.getAttribute(SecurityErrorAttributes.AUTH_ERROR_MESSAGE);

        if (code == null || message == null) {
            code = SecurityErrorAttributes.CODE_AUTH_REQUIRED;
            message = SecurityErrorAttributes.MESSAGE_AUTH_REQUIRED;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(code, message));
    }
}
