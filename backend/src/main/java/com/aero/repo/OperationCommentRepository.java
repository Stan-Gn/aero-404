package com.aero.repo;

import com.aero.domain.OperationComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationCommentRepository extends JpaRepository<OperationComment, Long> {
    List<OperationComment> findByOperationIdOrderByCreatedAtDesc(Long operationId);
}
