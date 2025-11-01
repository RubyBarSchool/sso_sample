package com.example.app.config;

import com.example.app.entity.Provider;
import com.example.app.entity.Role;
import com.example.app.entity.User;
import com.example.app.repository.RoleRepository;
import com.example.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedUsers();
    }

    private void seedRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(null, "ROLE_USER"));
            roleRepository.save(new Role(null, "ROLE_ADMIN"));
        }
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow();
            Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow();

            User admin = User.builder()
                .email("admin@local.dev")
                .username("Admin User")
                .password(passwordEncoder.encode("Admin@123"))
                .provider(Provider.LOCAL)
                .enabled(true)
                .build();
            admin.getRoles().add(adminRole);
            admin.getRoles().add(userRole);
            userRepository.save(admin);

            User user = User.builder()
                .email("user@local.dev")
                .username("Regular User")
                .password(passwordEncoder.encode("User@123"))
                .provider(Provider.LOCAL)
                .enabled(true)
                .build();
            user.getRoles().add(userRole);
            userRepository.save(user);
        }
    }
}

