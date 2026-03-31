package com.aero.service;

import com.aero.domain.AppUser;
import com.aero.domain.OperationComment;
import com.aero.domain.OperationHistory;
import com.aero.domain.OperationStatus;
import com.aero.domain.PlannedOperation;
import com.aero.domain.UserRole;
import com.aero.dto.OperationCommentDto;
import com.aero.dto.OperationHistoryDto;
import com.aero.dto.PlannedOperationRequestDto;
import com.aero.dto.PlannedOperationResponseDto;
import com.aero.dto.UserSimpleDto;
import com.aero.exception.EntityNotFoundException;
import com.aero.exception.ValidationException;
import com.aero.repo.AppUserRepository;
import com.aero.repo.OperationCommentRepository;
import com.aero.repo.OperationHistoryRepository;
import com.aero.repo.PlannedOperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class PlannedOperationService {

    private static final Logger log = LoggerFactory.getLogger(PlannedOperationService.class);

    @Autowired
    private PlannedOperationRepository operationRepository;

    @Autowired
    private OperationCommentRepository commentRepository;

    @Autowired
    private OperationHistoryRepository historyRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private KmlParserService kmlParserService;

    @Autowired
    private RouteCalculatorService routeCalculatorService;

    @Transactional(readOnly = true)
    public List<PlannedOperationResponseDto> findAll(OperationStatus filter) {
        List<PlannedOperation> operations;
        if (filter != null) {
            operations = operationRepository.findByStatusOrderByPlannedDateFromAsc(filter);
        } else {
            operations = operationRepository.findByStatusOrderByPlannedDateFromAsc(OperationStatus.CONFIRMED);
        }
        return operations.stream().map(this::mapToResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PlannedOperationResponseDto> findAll() {
        return operationRepository.findAllByOrderByPlannedDateFromAsc().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlannedOperationResponseDto findById(Long id) {
        PlannedOperation operation = operationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PlannedOperation", id));
        return mapToResponseDto(operation);
    }

    @Transactional
    public PlannedOperationResponseDto create(PlannedOperationRequestDto dto, String kmlContent) {
        if (kmlContent == null || kmlContent.isBlank()) {
            throw new ValidationException("KML file is required for planned operation");
        }
        validateRequestDates(dto);

        AppUser currentUser = getCurrentUser();

        PlannedOperation operation = PlannedOperation.builder()
                .autoNumber(generateAutoNumber())
                .orderNumber(dto.orderNumber())
                .shortDescription(dto.shortDescription())
                .kmlFileName(null)
                .kmlContent(kmlContent)
                .proposedDateFrom(dto.proposedDateFrom())
                .proposedDateTo(dto.proposedDateTo())
                .plannedDateFrom(null)
                .plannedDateTo(null)
                .activityTypes(dto.activityTypes())
                .additionalInfo(dto.additionalInfo())
                .status(OperationStatus.INTRODUCED)
                .createdBy(currentUser)
                .contactEmails(dto.contactEmails())
                .remarksAfterExecution(null)
                .build();

        if (kmlContent != null && !kmlContent.isBlank()) {
            List<double[]> points = kmlParserService.parse(kmlContent);
            operation.setRouteKm(routeCalculatorService.calculateRouteKm(points));
            operation.setRoutePoints(routeCalculatorService.pointsToJson(points));
        }

        PlannedOperation saved = operationRepository.save(operation);
        log.info("Created PlannedOperation with id {} and autoNumber {}", saved.getId(), saved.getAutoNumber());
        return mapToResponseDto(saved);
    }

    @Transactional
    public PlannedOperationResponseDto update(Long id, PlannedOperationRequestDto dto, String kmlContent) {
        PlannedOperation operation = operationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PlannedOperation", id));

        validateRequestDates(dto);
        AppUser currentUser = getCurrentUser();
        UserRole currentRole = currentUser.getRole();

        // PLANNER ograniczenia: nie może edytować plannedDates i remarksAfterExecution
        if (currentRole == UserRole.PLANNER) {
            // Sprawdzaj czy to jest własna operacja i status pozwala na edycję
            if (!operation.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new ValidationException("PLANNER can only edit own operations");
            }
            if (!isEditableByPlanner(operation.getStatus())) {
                throw new ValidationException("PLANNER cannot edit operations in status " + operation.getStatus());
            }
        }

        // Zmiana orderNumber
        if (!Objects.equals(operation.getOrderNumber(), dto.orderNumber())) {
            trackChange(operation, "orderNumber", operation.getOrderNumber(), dto.orderNumber(), currentUser);
            operation.setOrderNumber(dto.orderNumber());
        }

        // Zmiana shortDescription
        if (!Objects.equals(operation.getShortDescription(), dto.shortDescription())) {
            trackChange(operation, "shortDescription", operation.getShortDescription(), dto.shortDescription(), currentUser);
            operation.setShortDescription(dto.shortDescription());
        }

        // Zmiana activityTypes
        if (!Objects.equals(operation.getActivityTypes(), dto.activityTypes())) {
            trackChange(operation, "activityTypes",
                    String.valueOf(operation.getActivityTypes()),
                    String.valueOf(dto.activityTypes()),
                    currentUser);
            operation.setActivityTypes(dto.activityTypes());
        }

        // Zmiana additionalInfo
        if (!Objects.equals(operation.getAdditionalInfo(), dto.additionalInfo())) {
            trackChange(operation, "additionalInfo", operation.getAdditionalInfo(), dto.additionalInfo(), currentUser);
            operation.setAdditionalInfo(dto.additionalInfo());
        }

        // Zmiana contactEmails
        if (!Objects.equals(operation.getContactEmails(), dto.contactEmails())) {
            trackChange(operation, "contactEmails", operation.getContactEmails(), dto.contactEmails(), currentUser);
            operation.setContactEmails(dto.contactEmails());
        }

        // KML — tylko jeśli podano nowy plik
        if (kmlContent != null && !kmlContent.isBlank()) {
            operation.setKmlContent(kmlContent);
            List<double[]> points = kmlParserService.parse(kmlContent);
            int newRouteKm = routeCalculatorService.calculateRouteKm(points);
            if (!Objects.equals(operation.getRouteKm(), newRouteKm)) {
                trackChange(operation, "routeKm",
                        String.valueOf(operation.getRouteKm()),
                        String.valueOf(newRouteKm),
                        currentUser);
                operation.setRouteKm(newRouteKm);
            }
            operation.setRoutePoints(routeCalculatorService.pointsToJson(points));
        }

        // proposedDates mogą być zmieniane przez każdego (poza remarksAfterExecution dla PLANNER)
        if (!Objects.equals(operation.getProposedDateFrom(), dto.proposedDateFrom())) {
            trackChange(operation, "proposedDateFrom",
                    String.valueOf(operation.getProposedDateFrom()),
                    String.valueOf(dto.proposedDateFrom()),
                    currentUser);
            operation.setProposedDateFrom(dto.proposedDateFrom());
        }

        if (!Objects.equals(operation.getProposedDateTo(), dto.proposedDateTo())) {
            trackChange(operation, "proposedDateTo",
                    String.valueOf(operation.getProposedDateTo()),
                    String.valueOf(dto.proposedDateTo()),
                    currentUser);
            operation.setProposedDateTo(dto.proposedDateTo());
        }

        PlannedOperation saved = operationRepository.save(operation);
        log.info("Updated PlannedOperation with id {}", saved.getId());
        return mapToResponseDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        PlannedOperation operation = operationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PlannedOperation", id));
        operationRepository.delete(operation);
        log.info("Deleted PlannedOperation with id {}", id);
    }

    @Transactional
    public PlannedOperationResponseDto reject(Long id) {
        PlannedOperation operation = operationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PlannedOperation", id));

        if (operation.getStatus() != OperationStatus.INTRODUCED) {
            throw new ValidationException("Can only reject operations in INTRODUCED status");
        }

        AppUser currentUser = getCurrentUser();
        trackChange(operation, "status", String.valueOf(OperationStatus.INTRODUCED), String.valueOf(OperationStatus.REJECTED), currentUser);
        operation.setStatus(OperationStatus.REJECTED);

        PlannedOperation saved = operationRepository.save(operation);
        log.info("Rejected PlannedOperation with id {}", id);
        return mapToResponseDto(saved);
    }

    @Transactional
    public PlannedOperationResponseDto confirmToPlan(Long id, LocalDate plannedDateFrom, LocalDate plannedDateTo) {
        PlannedOperation operation = operationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PlannedOperation", id));

        if (operation.getStatus() != OperationStatus.INTRODUCED) {
            throw new ValidationException("Can only confirm operations in INTRODUCED status");
        }

        if (plannedDateFrom == null || plannedDateTo == null) {
            throw new ValidationException("Planned dates are required for confirmation");
        }

        if (plannedDateFrom.isAfter(plannedDateTo)) {
            throw new ValidationException("Planned date from must be before planned date to");
        }

        AppUser currentUser = getCurrentUser();

        trackChange(operation, "status", String.valueOf(OperationStatus.INTRODUCED), String.valueOf(OperationStatus.CONFIRMED), currentUser);
        trackChange(operation, "plannedDateFrom", String.valueOf(operation.getPlannedDateFrom()), String.valueOf(plannedDateFrom), currentUser);
        trackChange(operation, "plannedDateTo", String.valueOf(operation.getPlannedDateTo()), String.valueOf(plannedDateTo), currentUser);

        operation.setStatus(OperationStatus.CONFIRMED);
        operation.setPlannedDateFrom(plannedDateFrom);
        operation.setPlannedDateTo(plannedDateTo);

        PlannedOperation saved = operationRepository.save(operation);
        log.info("Confirmed PlannedOperation with id {} to CONFIRMED status", id);
        return mapToResponseDto(saved);
    }

    @Transactional
    public PlannedOperationResponseDto resign(Long id) {
        PlannedOperation operation = operationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PlannedOperation", id));

        OperationStatus currentStatus = operation.getStatus();
        if (currentStatus != OperationStatus.INTRODUCED
                && currentStatus != OperationStatus.CONFIRMED
                && currentStatus != OperationStatus.SCHEDULED) {
            throw new ValidationException("Can only resign from operations in INTRODUCED, CONFIRMED, or SCHEDULED status");
        }

        AppUser currentUser = getCurrentUser();
        trackChange(operation, "status", String.valueOf(currentStatus), String.valueOf(OperationStatus.RESIGNED), currentUser);
        operation.setStatus(OperationStatus.RESIGNED);

        PlannedOperation saved = operationRepository.save(operation);
        log.info("Resigned from PlannedOperation with id {}", id);
        return mapToResponseDto(saved);
    }

    @Transactional
    public OperationCommentDto addComment(Long operationId, String text) {
        PlannedOperation operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new EntityNotFoundException("PlannedOperation", operationId));

        if (text == null || text.isBlank()) {
            throw new ValidationException("Comment text cannot be empty");
        }

        AppUser currentUser = getCurrentUser();

        OperationComment comment = OperationComment.builder()
                .operation(operation)
                .text(text)
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        OperationComment saved = commentRepository.save(comment);
        log.info("Added comment to PlannedOperation with id {}", operationId);
        return mapCommentToDto(saved);
    }

    private void trackChange(PlannedOperation operation, String fieldName, String oldValue, String newValue, AppUser changedBy) {
        if (!Objects.equals(oldValue, newValue)) {
            OperationHistory history = OperationHistory.builder()
                    .operation(operation)
                    .fieldName(fieldName)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .changedBy(changedBy)
                    .changedAt(LocalDateTime.now())
                    .build();
            historyRepository.save(history);
        }
    }

    private boolean isEditableByPlanner(OperationStatus status) {
        return status == OperationStatus.INTRODUCED
                || status == OperationStatus.REJECTED
                || status == OperationStatus.CONFIRMED
                || status == OperationStatus.SCHEDULED
                || status == OperationStatus.PARTIALLY_DONE;
    }

    private AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User with email " + email + " not found"));
    }

    private void validateRequestDates(PlannedOperationRequestDto dto) {
        if (dto.proposedDateFrom() != null && dto.proposedDateTo() != null
                && dto.proposedDateFrom().isAfter(dto.proposedDateTo())) {
            throw new ValidationException("Proposed date from must be before proposed date to");
        }
    }

    private String generateAutoNumber() {
        return "OP-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PlannedOperationResponseDto mapToResponseDto(PlannedOperation operation) {
        List<OperationCommentDto> comments = commentRepository.findByOperationIdOrderByCreatedAtDesc(operation.getId())
                .stream()
                .map(this::mapCommentToDto)
                .toList();

        List<OperationHistoryDto> history = historyRepository.findByOperationIdOrderByChangedAtDesc(operation.getId())
                .stream()
                .map(this::mapHistoryToDto)
                .toList();

        return new PlannedOperationResponseDto(
                operation.getId(),
                operation.getAutoNumber(),
                operation.getOrderNumber(),
                operation.getShortDescription(),
                operation.getKmlFileName(),
                operation.getRouteKm(),
                operation.getRoutePoints(),
                operation.getProposedDateFrom(),
                operation.getProposedDateTo(),
                operation.getPlannedDateFrom(),
                operation.getPlannedDateTo(),
                operation.getActivityTypes(),
                operation.getAdditionalInfo(),
                operation.getStatus(),
                mapUserToSimpleDto(operation.getCreatedBy()),
                operation.getContactEmails(),
                operation.getRemarksAfterExecution(),
                comments,
                history
        );
    }

    private OperationCommentDto mapCommentToDto(OperationComment comment) {
        return new OperationCommentDto(
                comment.getId(),
                comment.getText(),
                mapUserToSimpleDto(comment.getCreatedBy()),
                comment.getCreatedAt()
        );
    }

    private OperationHistoryDto mapHistoryToDto(OperationHistory history) {
        return new OperationHistoryDto(
                history.getId(),
                history.getFieldName(),
                history.getOldValue(),
                history.getNewValue(),
                mapUserToSimpleDto(history.getChangedBy()),
                history.getChangedAt()
        );
    }

    private UserSimpleDto mapUserToSimpleDto(AppUser user) {
        return new UserSimpleDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
