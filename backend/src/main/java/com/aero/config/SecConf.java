package com.aero.config;

import com.aero.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecConf {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Administracja (PRD 7.2): ADMIN pełny, SUPERVISOR/PILOT podgląd, PLANNER brak
                        .requestMatchers("/api/v1/helicopters/**").hasAnyRole("ADMIN", "SUPERVISOR", "PILOT")
                        .requestMatchers("/api/v1/crew-members/**").hasAnyRole("ADMIN", "SUPERVISOR", "PILOT")
                        .requestMatchers("/api/v1/airfields/**").hasAnyRole("ADMIN", "SUPERVISOR", "PILOT")
                        .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "SUPERVISOR", "PILOT")
                        // Planowanie operacji (PRD 7.2): wszystkie role mają jakiś dostęp
                        .requestMatchers("/api/v1/operations/**").hasAnyRole("ADMIN", "PLANNER", "SUPERVISOR", "PILOT")
                        // Zlecenia na lot (PRD 7.2): PILOT/SUPERVISOR/ADMIN, PLANNER brak
                        .requestMatchers("/api/v1/flight-orders/**").hasAnyRole("ADMIN", "PILOT", "SUPERVISOR")
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
