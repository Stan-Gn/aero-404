package com.aero.repo;

import com.aero.domain.Airfield;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AirfieldRepository extends JpaRepository<Airfield, Long> {
    List<Airfield> findAllByOrderByNameAsc();
}
