package com.nfcattendance.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NfcTagPayloadTest {

    @Test
    void onePayloadWorksForIphoneAndGalaxy() {
        String payload = NfcTagPayload.unified(
                "https://nfcapp-nine.vercel.app/",
                "gate-01",
                "NFC-ABC");

        assertThat(payload).isEqualTo(
                "https://nfcapp-nine.vercel.app/?station=GATE-01#nfcattendance://station/GATE-01?rv=NFC-ABC");
        assertThat(payload).startsWith("https://");
        assertThat(payload).contains("nfcattendance://station/GATE-01");
    }
}
