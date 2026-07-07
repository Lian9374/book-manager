package com.bookmanager.config;

import com.bookmanager.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public auth endpoints
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                // Book & category browsing (read-only) is public
                .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                // Book write operations require LIBRARIAN or ADMIN
                .requestMatchers(HttpMethod.POST, "/api/books/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/books/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasAnyRole("LIBRARIAN", "ADMIN")
                // Category write operations require ADMIN
                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                // Borrow operations
                .requestMatchers(HttpMethod.POST, "/api/borrows").hasRole("READER")
                .requestMatchers("/api/borrows/*/return").hasAnyRole("LIBRARIAN", "ADMIN")
                // Reservations
                .requestMatchers(HttpMethod.POST, "/api/reservations").hasRole("READER")
                // Fines management
                .requestMatchers("/api/fines/*/pay").hasAnyRole("LIBRARIAN", "ADMIN")
                // User management
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                // Statistics
                .requestMatchers("/api/statistics/**").hasAnyRole("LIBRARIAN", "ADMIN")
                // Config
                .requestMatchers("/api/config/**").hasRole("ADMIN")
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
