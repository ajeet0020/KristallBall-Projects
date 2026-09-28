package com.millity.assets.config;

import com.millity.assets.domain.Role;
import com.millity.assets.domain.User;
import com.millity.assets.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(1)
public class BootstrapAdmin implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    public BootstrapAdmin(UserRepository users, PasswordEncoder encoder,
                          @Value("${app.bootstrap-admin.username:}") String username,
                          @Value("${app.bootstrap-admin.password:}") String password) {
        this.users=users; this.encoder=encoder; this.username=username; this.password=password;
    }
    @Override public void run(String... args) {
        if (!StringUtils.hasText(username) && !StringUtils.hasText(password)) return;
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password))
            throw new IllegalStateException("Set both BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD or neither");
        if (!users.existsByUsername(username)) {
            users.save(new User("System Administrator", username, encoder.encode(password), Role.ADMIN, null));
        }
    }
}
