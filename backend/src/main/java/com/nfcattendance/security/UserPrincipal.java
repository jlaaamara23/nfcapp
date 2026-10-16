package com.nfcattendance.security;

import com.nfcattendance.entity.RoleName;
import com.nfcattendance.entity.UserAccount;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final boolean locked;
    private final Set<RoleName> roleNames;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UserAccount user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.enabled = user.isEnabled();
        this.locked = user.isLocked() || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.Instant.now()));
        this.roleNames = user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet());
        this.authorities = roleNames.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .collect(Collectors.toSet());
    }

    public boolean hasRole(RoleName role) {
        return roleNames.contains(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
