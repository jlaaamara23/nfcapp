package com.nfcattendance.repository;

import com.nfcattendance.entity.NfcStation;
import com.nfcattendance.entity.NfcStationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NfcStationRepository extends JpaRepository<NfcStation, Long> {
    Optional<NfcStation> findByStationCode(String stationCode);
    Optional<NfcStation> findByRegistrationValue(String registrationValue);
    List<NfcStation> findByStatus(NfcStationStatus status);
    boolean existsByStationCode(String stationCode);
}
