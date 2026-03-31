package com.aero.service;

import com.aero.domain.OperationStatus;
import com.aero.domain.PlannedOperation;
import com.aero.exception.EntityNotFoundException;
import com.aero.repo.PlannedOperationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("operationSecurity")
public class OperationSecurityService {

    @Autowired
    private PlannedOperationRepository operationRepository;

    public boolean isOwner(Long operationId, String email) {
        PlannedOperation operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new EntityNotFoundException("PlannedOperation", operationId));
        return operation.getCreatedBy().getEmail().equals(email);
    }

    public boolean isEditableByPlanner(Long operationId) {
        PlannedOperation operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new EntityNotFoundException("PlannedOperation", operationId));
        OperationStatus status = operation.getStatus();
        return status == OperationStatus.INTRODUCED
                || status == OperationStatus.REJECTED
                || status == OperationStatus.CONFIRMED
                || status == OperationStatus.SCHEDULED
                || status == OperationStatus.PARTIALLY_DONE;
    }
}
