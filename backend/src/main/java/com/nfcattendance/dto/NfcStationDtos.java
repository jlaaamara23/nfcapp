package com.nfcattendance.dto;

import com.nfcattendance.entity.NfcStationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public final class NfcStationDtos {
    private NfcStationDtos() {}

    public record CreateRequest(
            @NotBlank String stationCode,
            @NotBlank String name,
            @NotNull Long workplaceId,
            String tagUidHint
    ) {}

    public record StatusUpdateRequest(
            @NotNull NfcStationStatus status,
            Long replacedById
    ) {}

    @Builder
    public record StationResponse(
            Long id,
            String stationCode,
            String name,
            Long workplaceId,
            String workplaceName,
            String registrationValue,
            String nfcPayload,
            String tagUidHint,
            NfcStationStatus status,
            Long replacedById
    ) {}
}
