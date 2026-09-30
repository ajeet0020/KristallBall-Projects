package com.millity.assets.security;

import com.millity.assets.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public CustomUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return new JwtPrincipal(user.getId(), user.getUsername(), user.getPassword(), user.getRole(), user.getAssignedBase() == null ? null : user.getAssignedBase().getId());
    }
}
