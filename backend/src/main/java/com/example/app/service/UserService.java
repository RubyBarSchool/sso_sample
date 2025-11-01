package com.example.app.service;

import com.example.app.dto.UserResponse;
import com.example.app.entity.Provider;
import com.example.app.entity.Role;
import com.example.app.entity.User;
import com.example.app.repository.RoleRepository;
import com.example.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword() != null ? user.getPassword() : "")
            .authorities(authorities)
            .disabled(!user.getEnabled())
            .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return toResponse(user);
    }

    @Transactional
    public User upsertOidcUser(String email, String name, String provider) {
        return userRepository.findByEmail(email)
            .orElseGet(() -> {
                User newUser = User.builder()
                    .email(email)
                    .username(name)
                    .provider(Provider.valueOf(provider))
                    .enabled(true)
                    .build();

                Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
                newUser.getRoles().add(userRole);

                return userRepository.save(newUser);
            });
    }

    @Transactional(readOnly = true)
    public java.util.List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::toResponse)
            .collect(java.util.stream.Collectors.toList());
    }

    private UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            user.getProvider(),
            user.getEnabled(),
            user.getCreatedAt(),
            roles
        );
    }
}

