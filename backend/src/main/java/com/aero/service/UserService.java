package com.aero.service;

import com.aero.domain.AppUser;
import com.aero.domain.UserRole;
import com.aero.dto.UserRequestDto;
import com.aero.dto.UserResponseDto;
import com.aero.exception.EntityNotFoundException;
import com.aero.exception.ValidationException;
import com.aero.repo.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserResponseDto> findAll() {
        return appUserRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    public UserResponseDto findById(Long id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
        return mapToResponseDto(user);
    }

    @Transactional
    public UserResponseDto create(UserRequestDto dto) {
        if (appUserRepository.existsByEmail(dto.email())) {
            throw new ValidationException("Email already exists");
        }

        AppUser user = AppUser.builder()
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .role(dto.role())
                .build();

        AppUser saved = appUserRepository.save(user);
        return mapToResponseDto(saved);
    }

    @Transactional
    public UserResponseDto update(Long id, UserRequestDto dto) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));

        if (!user.getEmail().equals(dto.email()) && appUserRepository.existsByEmail(dto.email())) {
            throw new ValidationException("Email already exists");
        }

        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(dto.role());

        AppUser saved = appUserRepository.save(user);
        return mapToResponseDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
        appUserRepository.delete(user);
    }

    @Transactional
    public UserResponseDto assignRole(Long id, UserRole role) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
        user.setRole(role);
        AppUser saved = appUserRepository.save(user);
        return mapToResponseDto(saved);
    }

    private UserResponseDto mapToResponseDto(AppUser user) {
        return new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
