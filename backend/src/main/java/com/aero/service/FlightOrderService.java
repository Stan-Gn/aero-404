package com.aero.service;

import com.aero.domain.*;
import com.aero.dto.*;
import com.aero.exception.EntityNotFoundException;
import com.aero.exception.ValidationException;
import com.aero.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlightOrderService {

    private static final Logger log = LoggerFactory.getLogger(FlightOrderService.class);

    @Autowired
    private FlightOrderRepository flightOrderRepository;

    @Autowired
    private PlannedOperationRepository operationRepository;

    @Autowired
    private CrewMemberRepository crewMemberRepository;

    @Autowired
    private HelicopterRepository helicopterRepository;

    @Autowired
    private AirfieldRepository airfieldRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private FlightOrderValidationService validationService;

    public List<FlightOrderResponseDto> findAll(FlightOrderStatus filter) {
        List<FlightOrder> orders;
        if (filter != null) {
            orders = flightOrderRepository.findByStatusOrderByPlannedDepartureAsc(filter);
        } else {
            orders = flightOrderRepository.findByStatusOrderByPlannedDepartureAsc(FlightOrderStatus.SUBMITTED);
        }
        return orders.stream().map(this::mapToResponseDto).toList();
    }

    public List<FlightOrderResponseDto> findAll() {
        return flightOrderRepository.findAllByOrderByPlannedDepartureAsc().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    public FlightOrderResponseDto findById(Long id) {
        FlightOrder order = flightOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FlightOrder", id));
        return mapToResponseDto(order);
    }

    @Transactional
    public FlightOrderResponseDto create(FlightOrderRequestDto dto) {
        AppUser currentUser = getCurrentUser();

        Helicopter helicopter = helicopterRepository.findById(dto.helicopterId())
                .orElseThrow(() -> new EntityNotFoundException("Helicopter", dto.helicopterId()));
        if (helicopter.getStatus() != HelicopterStatus.ACTIVE) {
            throw new ValidationException("Helicopter must be in ACTIVE status");
        }

        CrewMember pilot = resolvePilot(dto.pilotId(), currentUser);

        Set<CrewMember> crewMembers = resolveCrewMembers(dto.crewMemberIds());

        Set<PlannedOperation> operations = resolveOperations(dto.operationIds());
        for (PlannedOperation op : operations) {
            if (op.getStatus() != OperationStatus.CONFIRMED) {
                throw new ValidationException("Operation " + op.getAutoNumber() + " must be in CONFIRMED status");
            }
        }

        Airfield departureAirfield = airfieldRepository.findById(dto.departureAirfieldId())
                .orElseThrow(() -> new EntityNotFoundException("Airfield", dto.departureAirfieldId()));
        Airfield arrivalAirfield = airfieldRepository.findById(dto.arrivalAirfieldId())
                .orElseThrow(() -> new EntityNotFoundException("Airfield", dto.arrivalAirfieldId()));

        Integer crewWeight = calculateCrewWeight(pilot, crewMembers);

        FlightOrder order = FlightOrder.builder()
                .autoNumber(generateAutoNumber())
                .plannedDeparture(dto.plannedDeparture())
                .plannedLanding(dto.plannedLanding())
                .actualDeparture(dto.actualDeparture())
                .actualLanding(dto.actualLanding())
                .pilot(pilot)
                .crewMembers(crewMembers)
                .helicopter(helicopter)
                .departureAirfield(departureAirfield)
                .arrivalAirfield(arrivalAirfield)
                .plannedOperations(operations)
                .crewWeight(crewWeight)
                .estimatedRouteKm(dto.estimatedRouteKm())
                .status(FlightOrderStatus.INTRODUCED)
                .build();

        validationService.validate(order);

        // Zmień status operacji: CONFIRMED(3) → SCHEDULED(4)
        for (PlannedOperation op : operations) {
            op.setStatus(OperationStatus.SCHEDULED);
            operationRepository.save(op);
        }

        FlightOrder saved = flightOrderRepository.save(order);
        log.info("Created FlightOrder with id {} and autoNumber {}", saved.getId(), saved.getAutoNumber());
        return mapToResponseDto(saved);
    }

    @Transactional
    public FlightOrderResponseDto update(Long id, FlightOrderRequestDto dto) {
        FlightOrder order = flightOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FlightOrder", id));

        AppUser currentUser = getCurrentUser();

        Helicopter helicopter = helicopterRepository.findById(dto.helicopterId())
                .orElseThrow(() -> new EntityNotFoundException("Helicopter", dto.helicopterId()));
        if (helicopter.getStatus() != HelicopterStatus.ACTIVE) {
            throw new ValidationException("Helicopter must be in ACTIVE status");
        }

        CrewMember pilot = resolvePilot(dto.pilotId(), currentUser);
        Set<CrewMember> crewMembers = resolveCrewMembers(dto.crewMemberIds());

        Set<PlannedOperation> newOperations = resolveOperations(dto.operationIds());
        for (PlannedOperation op : newOperations) {
            if (op.getStatus() != OperationStatus.CONFIRMED && op.getStatus() != OperationStatus.SCHEDULED) {
                throw new ValidationException("Operation " + op.getAutoNumber() + " must be in CONFIRMED or SCHEDULED status");
            }
        }

        Airfield departureAirfield = airfieldRepository.findById(dto.departureAirfieldId())
                .orElseThrow(() -> new EntityNotFoundException("Airfield", dto.departureAirfieldId()));
        Airfield arrivalAirfield = airfieldRepository.findById(dto.arrivalAirfieldId())
                .orElseThrow(() -> new EntityNotFoundException("Airfield", dto.arrivalAirfieldId()));

        // Cofnij stare operacje do CONFIRMED (jeśli nie są w nowym zestawie)
        Set<Long> newOperationIds = newOperations.stream().map(PlannedOperation::getId).collect(Collectors.toSet());
        for (PlannedOperation oldOp : order.getPlannedOperations()) {
            if (!newOperationIds.contains(oldOp.getId()) && oldOp.getStatus() == OperationStatus.SCHEDULED) {
                oldOp.setStatus(OperationStatus.CONFIRMED);
                operationRepository.save(oldOp);
            }
        }

        // Ustaw nowe operacje na SCHEDULED
        for (PlannedOperation op : newOperations) {
            if (op.getStatus() == OperationStatus.CONFIRMED) {
                op.setStatus(OperationStatus.SCHEDULED);
                operationRepository.save(op);
            }
        }

        Integer crewWeight = calculateCrewWeight(pilot, crewMembers);

        order.setPlannedDeparture(dto.plannedDeparture());
        order.setPlannedLanding(dto.plannedLanding());
        order.setActualDeparture(dto.actualDeparture());
        order.setActualLanding(dto.actualLanding());
        order.setPilot(pilot);
        order.setCrewMembers(crewMembers);
        order.setHelicopter(helicopter);
        order.setDepartureAirfield(departureAirfield);
        order.setArrivalAirfield(arrivalAirfield);
        order.setPlannedOperations(newOperations);
        order.setCrewWeight(crewWeight);
        order.setEstimatedRouteKm(dto.estimatedRouteKm());

        validationService.validate(order);

        FlightOrder saved = flightOrderRepository.save(order);
        log.info("Updated FlightOrder with id {}", saved.getId());
        return mapToResponseDto(saved);
    }

    @Transactional
    public FlightOrderResponseDto submit(Long id) {
        FlightOrder order = flightOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FlightOrder", id));

        if (order.getStatus() != FlightOrderStatus.INTRODUCED) {
            throw new ValidationException("Can only submit flight orders in INTRODUCED status");
        }

        order.setStatus(FlightOrderStatus.SUBMITTED);
        FlightOrder saved = flightOrderRepository.save(order);
        log.info("Submitted FlightOrder with id {}", id);
        return mapToResponseDto(saved);
    }

    @Transactional
    public FlightOrderResponseDto reject(Long id) {
        FlightOrder order = flightOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FlightOrder", id));

        if (order.getStatus() != FlightOrderStatus.SUBMITTED) {
            throw new ValidationException("Can only reject flight orders in SUBMITTED status");
        }

        order.setStatus(FlightOrderStatus.REJECTED);
        FlightOrder saved = flightOrderRepository.save(order);
        log.info("Rejected FlightOrder with id {}", id);
        return mapToResponseDto(saved);
    }

    @Transactional
    public FlightOrderResponseDto accept(Long id) {
        FlightOrder order = flightOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FlightOrder", id));

        if (order.getStatus() != FlightOrderStatus.SUBMITTED) {
            throw new ValidationException("Can only accept flight orders in SUBMITTED status");
        }

        order.setStatus(FlightOrderStatus.ACCEPTED);
        FlightOrder saved = flightOrderRepository.save(order);
        log.info("Accepted FlightOrder with id {}", id);
        return mapToResponseDto(saved);
    }

    @Transactional
    public FlightOrderResponseDto markPartiallyDone(Long id) {
        FlightOrder order = flightOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FlightOrder", id));

        if (order.getStatus() != FlightOrderStatus.ACCEPTED) {
            throw new ValidationException("Can only mark as partially done flight orders in ACCEPTED status");
        }
        validateActualTimes(order);

        order.setStatus(FlightOrderStatus.PARTIALLY_DONE);
        for (PlannedOperation op : order.getPlannedOperations()) {
            op.setStatus(OperationStatus.PARTIALLY_DONE);
            operationRepository.save(op);
        }

        FlightOrder saved = flightOrderRepository.save(order);
        log.info("Marked FlightOrder with id {} as PARTIALLY_DONE", id);
        return mapToResponseDto(saved);
    }

    @Transactional
    public FlightOrderResponseDto markDone(Long id) {
        FlightOrder order = flightOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FlightOrder", id));

        if (order.getStatus() != FlightOrderStatus.ACCEPTED) {
            throw new ValidationException("Can only mark as done flight orders in ACCEPTED status");
        }
        validateActualTimes(order);

        order.setStatus(FlightOrderStatus.DONE);
        for (PlannedOperation op : order.getPlannedOperations()) {
            op.setStatus(OperationStatus.DONE);
            operationRepository.save(op);
        }

        FlightOrder saved = flightOrderRepository.save(order);
        log.info("Marked FlightOrder with id {} as DONE", id);
        return mapToResponseDto(saved);
    }

    @Transactional
    public FlightOrderResponseDto markNotDone(Long id) {
        FlightOrder order = flightOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FlightOrder", id));

        if (order.getStatus() != FlightOrderStatus.ACCEPTED) {
            throw new ValidationException("Can only mark as not done flight orders in ACCEPTED status");
        }

        order.setStatus(FlightOrderStatus.NOT_DONE);
        for (PlannedOperation op : order.getPlannedOperations()) {
            op.setStatus(OperationStatus.CONFIRMED);
            operationRepository.save(op);
        }

        FlightOrder saved = flightOrderRepository.save(order);
        log.info("Marked FlightOrder with id {} as NOT_DONE", id);
        return mapToResponseDto(saved);
    }

    private void validateActualTimes(FlightOrder order) {
        if (order.getActualDeparture() == null || order.getActualLanding() == null) {
            throw new ValidationException("Actual departure and landing times are required before completing a flight order");
        }
    }

    private CrewMember resolvePilot(Long pilotId, AppUser currentUser) {
        if (pilotId != null) {
            CrewMember pilot = crewMemberRepository.findById(pilotId)
                    .orElseThrow(() -> new EntityNotFoundException("CrewMember", pilotId));
            if (pilot.getRole() != CrewRole.PILOT) {
                throw new ValidationException("Selected crew member must have PILOT role");
            }
            return pilot;
        }

        // Autouzupełnianie: jeśli zalogowany jest PILOT, znajdź CrewMember po email
        if (currentUser.getRole() == UserRole.PILOT) {
            return crewMemberRepository.findAllByOrderByEmailAsc().stream()
                    .filter(cm -> cm.getEmail().equals(currentUser.getEmail()) && cm.getRole() == CrewRole.PILOT)
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("No crew member with PILOT role found for current user email"));
        }

        throw new ValidationException("Pilot is required");
    }

    private Set<CrewMember> resolveCrewMembers(Set<Long> crewMemberIds) {
        if (crewMemberIds == null || crewMemberIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<CrewMember> members = new HashSet<>();
        for (Long memberId : crewMemberIds) {
            CrewMember member = crewMemberRepository.findById(memberId)
                    .orElseThrow(() -> new EntityNotFoundException("CrewMember", memberId));
            members.add(member);
        }
        return members;
    }

    private Set<PlannedOperation> resolveOperations(Set<Long> operationIds) {
        Set<PlannedOperation> operations = new HashSet<>();
        for (Long opId : operationIds) {
            PlannedOperation op = operationRepository.findById(opId)
                    .orElseThrow(() -> new EntityNotFoundException("PlannedOperation", opId));
            operations.add(op);
        }
        return operations;
    }

    private Integer calculateCrewWeight(CrewMember pilot, Set<CrewMember> crewMembers) {
        int weight = pilot.getWeight();
        if (crewMembers != null) {
            for (CrewMember member : crewMembers) {
                weight += member.getWeight();
            }
        }
        return weight;
    }

    private AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User with email " + email + " not found"));
    }

    private String generateAutoNumber() {
        return "FO-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private FlightOrderResponseDto mapToResponseDto(FlightOrder order) {
        List<CrewMemberResponseDto> crewMemberDtos = order.getCrewMembers() != null
                ? order.getCrewMembers().stream().map(this::mapCrewMemberToDto).toList()
                : List.of();

        List<PlannedOperationSimpleDto> operationDtos = order.getPlannedOperations() != null
                ? order.getPlannedOperations().stream().map(this::mapOperationToSimpleDto).toList()
                : List.of();

        return new FlightOrderResponseDto(
                order.getId(),
                order.getAutoNumber(),
                order.getPlannedDeparture(),
                order.getPlannedLanding(),
                order.getActualDeparture(),
                order.getActualLanding(),
                mapCrewMemberToDto(order.getPilot()),
                mapHelicopterToDto(order.getHelicopter()),
                crewMemberDtos,
                mapAirfieldToDto(order.getDepartureAirfield()),
                mapAirfieldToDto(order.getArrivalAirfield()),
                operationDtos,
                order.getCrewWeight(),
                order.getEstimatedRouteKm(),
                order.getStatus()
        );
    }

    private CrewMemberResponseDto mapCrewMemberToDto(CrewMember cm) {
        return new CrewMemberResponseDto(
                cm.getId(),
                cm.getFirstName(),
                cm.getLastName(),
                cm.getEmail(),
                cm.getWeight(),
                cm.getRole(),
                cm.getLicenseNumber(),
                cm.getLicenseExpiry(),
                cm.getTrainingExpiry()
        );
    }

    private HelicopterResponseDto mapHelicopterToDto(Helicopter h) {
        return new HelicopterResponseDto(
                h.getId(),
                h.getRegNumber(),
                h.getType(),
                h.getDescription(),
                h.getMaxCrew(),
                h.getMaxPayload(),
                h.getStatus(),
                h.getReviewDate(),
                h.getRangeKm()
        );
    }

    private AirfieldResponseDto mapAirfieldToDto(Airfield a) {
        return new AirfieldResponseDto(
                a.getId(),
                a.getName(),
                a.getLatitude(),
                a.getLongitude()
        );
    }

    private PlannedOperationSimpleDto mapOperationToSimpleDto(PlannedOperation op) {
        return new PlannedOperationSimpleDto(
                op.getId(),
                op.getAutoNumber(),
                op.getShortDescription(),
                op.getStatus(),
                op.getRouteKm()
        );
    }
}
