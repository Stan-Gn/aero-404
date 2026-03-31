package com.aero.repo;

import com.aero.domain.FlightOrder;
import com.aero.domain.FlightOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightOrderRepository extends JpaRepository<FlightOrder, Long> {
    List<FlightOrder> findByStatusOrderByPlannedDepartureAsc(FlightOrderStatus status);
    List<FlightOrder> findAllByOrderByPlannedDepartureAsc();
}
