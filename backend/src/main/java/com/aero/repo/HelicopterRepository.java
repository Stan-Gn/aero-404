package com.aero.repo;

import com.aero.domain.Helicopter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HelicopterRepository extends JpaRepository<Helicopter, Long> {
    Optional<Helicopter> findByRegNumber(String regNumber);

    boolean existsByRegNumber(String regNumber);

    List<Helicopter> findAllByOrderByStatusAscRegNumberAsc();
}
