package com.nfcattendance.service;

import com.nfcattendance.dto.NfcStationDtos;
import com.nfcattendance.entity.NfcStation;
import com.nfcattendance.entity.NfcStationStatus;
import com.nfcattendance.entity.Workplace;
import com.nfcattendance.exception.ApiException;
import com.nfcattendance.repository.NfcStationRepository;
import com.nfcattendance.repository.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NfcStationService {

    private final NfcStationRepository nfcStationRepository;
    private final WorkplaceRepository workplaceRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<NfcStationDtos.StationResponse> list() {
        return nfcStationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public NfcStationDtos.StationResponse create(NfcStationDtos.CreateRequest request) {
        if (nfcStationRepository.existsByStationCode(request.stationCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "קוד תחנה כבר קיים");
        }
        Workplace workplace = workplaceRepository.findById(request.workplaceId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "מיקום עבודה לא נמצא"));

        String registrationValue = "NFC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        NfcStation station = NfcStation.builder()
                .stationCode(request.stationCode().trim().toUpperCase())
                .name(request.name().trim())
                .workplace(workplace)
                .registrationValue(registrationValue)
                .tagUidHint(request.tagUidHint())
                .status(NfcStationStatus.ACTIVE)
                .build();
        station = nfcStationRepository.save(station);
        auditService.log("NFC_STATION_CREATE", "NfcStation", String.valueOf(station.getId()),
                "stationCode=" + station.getStationCode());
        return toResponse(station);
    }

    @Transactional
    public NfcStationDtos.StationResponse updateStatus(Long id, NfcStationDtos.StatusUpdateRequest request) {
        NfcStation station = nfcStationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "תחנת NFC לא נמצאה"));
        station.setStatus(request.status());
        if (request.status() == NfcStationStatus.REPLACED) {
            if (request.replacedById() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "חובה לציין תחנה מחליפה");
            }
            NfcStation replacement = nfcStationRepository.findById(request.replacedById())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "תחנה מחליפה לא נמצאה"));
            station.setReplacedBy(replacement);
        }
        station = nfcStationRepository.save(station);
        auditService.log("NFC_STATION_STATUS", "NfcStation", String.valueOf(id), "status=" + request.status());
        return toResponse(station);
    }

    private NfcStationDtos.StationResponse toResponse(NfcStation s) {
        String payload = "nfcattendance://station/" + s.getStationCode() + "?rv=" + s.getRegistrationValue();
        return NfcStationDtos.StationResponse.builder()
                .id(s.getId())
                .stationCode(s.getStationCode())
                .name(s.getName())
                .workplaceId(s.getWorkplace().getId())
                .workplaceName(s.getWorkplace().getName())
                .registrationValue(s.getRegistrationValue())
                .nfcPayload(payload)
                .tagUidHint(s.getTagUidHint())
                .status(s.getStatus())
                .replacedById(s.getReplacedBy() != null ? s.getReplacedBy().getId() : null)
                .build();
    }
}
