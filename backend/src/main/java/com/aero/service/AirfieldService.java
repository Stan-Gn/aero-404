package com.aero.service;

import com.aero.domain.Airfield;
import com.aero.dto.AirfieldRequestDto;
import com.aero.dto.AirfieldResponseDto;
import com.aero.exception.EntityNotFoundException;
import com.aero.repo.AirfieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AirfieldService {

    @Autowired
    private AirfieldRepository airfieldRepository;

    public List<AirfieldResponseDto> findAll() {
        return airfieldRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    public AirfieldResponseDto findById(Long id) {
        Airfield airfield = airfieldRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Airfield", id));
        return mapToResponseDto(airfield);
    }

    @Transactional
    public AirfieldResponseDto create(AirfieldRequestDto dto) {
        Airfield airfield = Airfield.builder()
                .name(dto.name())
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .build();

        Airfield saved = airfieldRepository.save(airfield);
        return mapToResponseDto(saved);
    }

    @Transactional
    public AirfieldResponseDto update(Long id, AirfieldRequestDto dto) {
        Airfield airfield = airfieldRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Airfield", id));

        airfield.setName(dto.name());
        airfield.setLatitude(dto.latitude());
        airfield.setLongitude(dto.longitude());

        Airfield saved = airfieldRepository.save(airfield);
        return mapToResponseDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Airfield airfield = airfieldRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Airfield", id));
        airfieldRepository.delete(airfield);
    }

    private AirfieldResponseDto mapToResponseDto(Airfield airfield) {
        return new AirfieldResponseDto(
                airfield.getId(),
                airfield.getName(),
                airfield.getLatitude(),
                airfield.getLongitude()
        );
    }
}
