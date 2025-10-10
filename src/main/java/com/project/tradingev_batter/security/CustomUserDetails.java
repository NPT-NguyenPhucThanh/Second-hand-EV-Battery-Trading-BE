package com.project.tradingev_batter.security;

import org.springframework.security.core.userdetails.UserDetails;
import com.project.tradingev_batter.Entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.stream.Collectors;

public class CustomUserDetails implements UserDetails {
    private final User user;
    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRolename().toUpperCase()))
                .collect(Collectors.toList());
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
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isIsactive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isIsactive();
    }

    // Add custom getters for claims
    public String getEmail() {
        return user.getEmail();
    }

    public String getDisplayname() {
        return user.getDisplayname();
    }

    // Add more if need (e.g., manager flag if role contains "MANAGER")
    public boolean isManager() {
        return getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_MANAGER"));
    }

    // Getter cho User entity
    public User getUser() {
        return user;
    }
}
