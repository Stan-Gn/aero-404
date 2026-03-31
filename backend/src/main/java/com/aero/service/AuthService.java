package com.aero.service;

import com.aero.domain.AppUser;
import com.aero.dto.LoginRequestDto;
import com.aero.dto.LoginResponseDto;
import com.aero.dto.RegisterRequestDto;
import com.aero.dto.RegisterResponseDto;
import com.aero.exception.ValidationException;
import com.aero.repo.AppUserRepository;
import com.aero.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto dto) {
        if (appUserRepository.existsByEmail(dto.email())) {
            throw new ValidationException("Email already exists");
        }

        AppUser appUser = AppUser.builder()
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .role(dto.role())
                .build();

        AppUser saved = appUserRepository.save(appUser);

        return new RegisterResponseDto(saved.getId(), saved.getEmail(), saved.getRole());
    }

    public LoginResponseDto login(LoginRequestDto dto) {
        AppUser appUser = appUserRepository.findByEmail(dto.email())
                .orElseThrow(() -> new ValidationException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.password(), appUser.getPassword())) {
            throw new ValidationException("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(appUser.getEmail(), appUser.getRole());

        return new LoginResponseDto(token, appUser.getEmail(), appUser.getRole());
    }
}
