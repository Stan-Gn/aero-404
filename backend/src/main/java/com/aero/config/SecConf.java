package com.aero.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecConf {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Całkowite wyłączenie ochrony CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // Całkowite wyłączenie formularza logowania (tego ze screena)
                .formLogin(AbstractHttpConfigurer::disable)
                // Wyłączenie uwierzytelniania HTTP Basic (okienko w przeglądarce)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Pozwolenie na KAŻDE zapytanie bez wyjątku
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
