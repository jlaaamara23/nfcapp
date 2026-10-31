package com.nfcattendance.config;

import com.nfcattendance.entity.AttendanceRules;
import com.nfcattendance.entity.NfcStation;
import com.nfcattendance.entity.NfcStationStatus;
import com.nfcattendance.entity.Workplace;
import com.nfcattendance.repository.AttendanceRulesRepository;
import com.nfcattendance.repository.NfcStationRepository;
import com.nfcattendance.repository.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final WorkplaceRepository workplaceRepository;
    private final NfcStationRepository nfcStationRepository;
    private final AttendanceRulesRepository attendanceRulesRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Workplace hq = workplaceRepository.findByCode("TLV-HQ").orElse(null);
        if (hq == null) {
            log.info("Skipping optional seed — workplaces not present");
            return;
        }
        if (nfcStationRepository.findByStationCode("GATE-01").isEmpty()) {
            NfcStation station = NfcStation.builder()
                    .stationCode("GATE-01")
                    .name("שער כניסה ראשי")
                    .workplace(hq)
                    .registrationValue("NFC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                    .status(NfcStationStatus.ACTIVE)
                    .build();
            nfcStationRepository.save(station);
            log.info("Seeded NFC station GATE-01 with registration value {}", station.getRegistrationValue());
        }
        if (attendanceRulesRepository.findByWorkplaceId(hq.getId()).isEmpty()) {
            attendanceRulesRepository.save(AttendanceRules.builder()
                    .workplace(hq)
                    .graceMinutes(10)
                    .overtimeAfterMinutes(480)
                    .allowOvernight(true)
                    .duplicateWindowSeconds(60)
                    .build());
        }
    }
}
