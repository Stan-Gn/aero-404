package com.aero.service;

import com.aero.domain.Helicopter;
import com.aero.domain.HelicopterStatus;
import com.aero.dto.HelicopterRequestDto;
import com.aero.dto.HelicopterResponseDto;
import com.aero.exception.EntityNotFoundException;
import com.aero.exception.ValidationException;
import com.aero.repo.HelicopterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HelicopterService {

    @Autowired
    private HelicopterRepository helicopterRepository;

    public List<HelicopterResponseDto> findAll() {
        return helicopterRepository.findAllByOrderByStatusAscRegNumberAsc().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    public HelicopterResponseDto findById(Long id) {
        Helicopter helicopter = helicopterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Helicopter", id));
        return mapToResponseDto(helicopter);
    }

    @Transactional
    public HelicopterResponseDto create(HelicopterRequestDto dto) {
        if (helicopterRepository.existsByRegNumber(dto.regNumber())) {
            throw new ValidationException("Helicopter with registration number " + dto.regNumber() + " already exists");
        }
        validateReviewDate(dto);

        Helicopter helicopter = Helicopter.builder()
                .regNumber(dto.regNumber())
                .type(dto.type())
                .description(dto.description())
                .maxCrew(dto.maxCrew())
                .maxPayload(dto.maxPayload())
                .status(dto.status())
                .reviewDate(dto.reviewDate())
                .rangeKm(dto.rangeKm())
                .build();

        Helicopter saved = helicopterRepository.save(helicopter);
        return mapToResponseDto(saved);
    }

    @Transactional
    public HelicopterResponseDto update(Long id, HelicopterRequestDto dto) {
        Helicopter helicopter = helicopterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Helicopter", id));

        if (!helicopter.getRegNumber().equals(dto.regNumber())
                && helicopterRepository.existsByRegNumber(dto.regNumber())) {
            throw new ValidationException("Helicopter with registration number " + dto.regNumber() + " already exists");
        }
        validateReviewDate(dto);

        helicopter.setRegNumber(dto.regNumber());
        helicopter.setType(dto.type());
        helicopter.setDescription(dto.description());
        helicopter.setMaxCrew(dto.maxCrew());
        helicopter.setMaxPayload(dto.maxPayload());
        helicopter.setStatus(dto.status());
        helicopter.setReviewDate(dto.reviewDate());
        helicopter.setRangeKm(dto.rangeKm());

        Helicopter saved = helicopterRepository.save(helicopter);
        return mapToResponseDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Helicopter helicopter = helicopterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Helicopter", id));
        helicopterRepository.delete(helicopter);
    }

    private void validateReviewDate(HelicopterRequestDto dto) {
        if (dto.status() == HelicopterStatus.ACTIVE && dto.reviewDate() == null) {
            throw new ValidationException("Review date is required when helicopter status is ACTIVE");
        }
    }

    private HelicopterResponseDto mapToResponseDto(Helicopter helicopter) {
        return new HelicopterResponseDto(
                helicopter.getId(),
                helicopter.getRegNumber(),
                helicopter.getType(),
                helicopter.getDescription(),
                helicopter.getMaxCrew(),
                helicopter.getMaxPayload(),
                helicopter.getStatus(),
                helicopter.getReviewDate(),
                helicopter.getRangeKm()
        );
    }
}
