package com.aero.repo;

import com.aero.domain.OperationStatus;
import com.aero.domain.PlannedOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlannedOperationRepository extends JpaRepository<PlannedOperation, Long> {
    List<PlannedOperation> findByStatusOrderByPlannedDateFromAsc(OperationStatus status);
    List<PlannedOperation> findAllByOrderByPlannedDateFromAsc();
}
