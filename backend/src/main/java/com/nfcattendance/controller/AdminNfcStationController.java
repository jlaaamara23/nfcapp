package com.nfcattendance.controller;

import com.nfcattendance.dto.NfcStationDtos;
import com.nfcattendance.service.NfcStationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/nfc-stations")
@RequiredArgsConstructor
@Tag(name = "Admin NFC Stations")
public class AdminNfcStationController {

    private final NfcStationService nfcStationService;

    @GetMapping
    public List<NfcStationDtos.StationResponse> list() {
        return nfcStationService.list();
    }

    @PostMapping
    public NfcStationDtos.StationResponse create(@Valid @RequestBody NfcStationDtos.CreateRequest request) {
        return nfcStationService.create(request);
    }

    @PatchMapping("/{id}/status")
    public NfcStationDtos.StationResponse updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody NfcStationDtos.StatusUpdateRequest request) {
        return nfcStationService.updateStatus(id, request);
    }
}
