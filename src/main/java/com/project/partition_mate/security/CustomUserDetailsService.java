package com.project.partition_mate.security;

import com.project.partition_mate.domain.User;
import com.project.partition_mate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    // 💡 메서드명은 변경할 수 없으므로, 인자(username)를 User ID 문자열로 간주합니다.
    public UserDetails loadUserByUsername(String userIdString) throws UsernameNotFoundException {

        Long userId;
        try {
            // 💡 1. 문자열로 받은 User ID를 Long으로 변환
            userId = Long.parseLong(userIdString);
        } catch (NumberFormatException e) {
            // 토큰의 Subject가 유효한 Long 값이 아닐 경우 처리
            throw new UsernameNotFoundException("유효하지 않은 User ID 포맷입니다: " + userIdString);
        }

        // 💡 2. DB에서 ID를 기준으로 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        // 조회 실패 시 UsernameNotFoundException 발생
                        new UsernameNotFoundException("해당 User ID를 찾을 수 없습니다: " + userIdString)
                );

        return new CustomUserDetails(user);
    }
}