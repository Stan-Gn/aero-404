package com.aero.repo;

import com.aero.domain.CrewMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {
    boolean existsByEmail(String email);

    List<CrewMember> findAllByOrderByEmailAsc();
}
