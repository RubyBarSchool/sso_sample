package com.example.app.security;

import com.example.app.service.JwtService;
import com.example.app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        String email = principal.getAttribute("email");
        if (email == null) {
            email = principal.getAttribute("preferred_username");
        }

        String name = principal.getAttribute("name");
        if (name == null) {
            name = email;
        }

        String provider;
        String clientName = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        if ("azure".equals(clientName)) {
            provider = "MICROSOFT";
        } else {
            provider = "GOOGLE";
        }

        var user = userService.upsertOidcUser(email, name, provider);

        Set<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(Collectors.toSet());

        String token = jwtService.issueToken(user.getEmail(), authorities);

        String redirectUrl = "http://localhost:5173/#/oauth/callback?token="
            + URLEncoder.encode(token, StandardCharsets.UTF_8)
            + "&provider=" + URLEncoder.encode(provider, StandardCharsets.UTF_8);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

