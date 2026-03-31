package com.aero.service;

import com.aero.domain.CrewMember;
import com.aero.domain.CrewRole;
import com.aero.dto.CrewMemberRequestDto;
import com.aero.dto.CrewMemberResponseDto;
import com.aero.exception.EntityNotFoundException;
import com.aero.exception.ValidationException;
import com.aero.repo.CrewMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CrewMemberService {

    @Autowired
    private CrewMemberRepository crewMemberRepository;

    public List<CrewMemberResponseDto> findAll() {
        return crewMemberRepository.findAllByOrderByEmailAsc().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    public CrewMemberResponseDto findById(Long id) {
        CrewMember member = crewMemberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CrewMember", id));
        return mapToResponseDto(member);
    }

    @Transactional
    public CrewMemberResponseDto create(CrewMemberRequestDto dto) {
        if (crewMemberRepository.existsByEmail(dto.email())) {
            throw new ValidationException("Email already exists");
        }
        validatePilotFields(dto);

        CrewMember member = CrewMember.builder()
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .email(dto.email())
                .weight(dto.weight())
                .role(dto.role())
                .licenseNumber(dto.licenseNumber())
                .licenseExpiry(dto.licenseExpiry())
                .trainingExpiry(dto.trainingExpiry())
                .build();

        CrewMember saved = crewMemberRepository.save(member);
        return mapToResponseDto(saved);
    }

    @Transactional
    public CrewMemberResponseDto update(Long id, CrewMemberRequestDto dto) {
        CrewMember member = crewMemberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CrewMember", id));

        if (!member.getEmail().equals(dto.email()) && crewMemberRepository.existsByEmail(dto.email())) {
            throw new ValidationException("Email already exists");
        }
        validatePilotFields(dto);

        member.setFirstName(dto.firstName());
        member.setLastName(dto.lastName());
        member.setEmail(dto.email());
        member.setWeight(dto.weight());
        member.setRole(dto.role());
        member.setLicenseNumber(dto.licenseNumber());
        member.setLicenseExpiry(dto.licenseExpiry());
        member.setTrainingExpiry(dto.trainingExpiry());

        CrewMember saved = crewMemberRepository.save(member);
        return mapToResponseDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        CrewMember member = crewMemberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CrewMember", id));
        crewMemberRepository.delete(member);
    }

    private void validatePilotFields(CrewMemberRequestDto dto) {
        if (dto.role() == CrewRole.PILOT) {
            if (dto.licenseNumber() == null || dto.licenseNumber().isBlank()) {
                throw new ValidationException("License number is required for role PILOT");
            }
            if (dto.licenseExpiry() == null) {
                throw new ValidationException("License expiry date is required for role PILOT");
            }
        }
    }

    private CrewMemberResponseDto mapToResponseDto(CrewMember member) {
        return new CrewMemberResponseDto(
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getWeight(),
                member.getRole(),
                member.getLicenseNumber(),
                member.getLicenseExpiry(),
                member.getTrainingExpiry()
        );
    }
}
