package com.nfcattendance.repository;

import com.nfcattendance.entity.Workplace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkplaceRepository extends JpaRepository<Workplace, Long> {
    Optional<Workplace> findByCode(String code);
    List<Workplace> findByActiveTrueOrderByNameAsc();
}
