package com.project.partition_mate.security;

import com.project.partition_mate.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class CustomUserDetails  implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return Collections
                .singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 안됨
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 안 잠김
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비번 만료 안됨
    }

    @Override
    public boolean isEnabled() {
        return true; // 계정 사용 가능
    }

    public User getUser() {
        return this.user;
    }

}
