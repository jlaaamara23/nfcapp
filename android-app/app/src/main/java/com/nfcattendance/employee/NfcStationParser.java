package com.nfcattendance.employee;

import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class NfcStationParser {
    private NfcStationParser() {}

    public static String parseStationCode(Intent intent) throws IllegalStateException {
        if (intent == null) {
            throw new IllegalStateException("לא התקבל אירוע NFC");
        }

        Parcelable[] raw = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (raw != null) {
            for (Parcelable p : raw) {
                NdefMessage msg = (NdefMessage) p;
                for (NdefRecord record : msg.getRecords()) {
                    String text = recordToText(record);
                    String code = extractStationCode(text);
                    if (code != null) {
                        return code;
                    }
                }
            }
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                try {
                    ndef.connect();
                    NdefMessage msg = ndef.getNdefMessage();
                    if (msg != null) {
                        for (NdefRecord record : msg.getRecords()) {
                            String text = recordToText(record);
                            String code = extractStationCode(text);
                            if (code != null) {
                                return code;
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("לא ניתן לקרוא את התג. נסו שוב.");
                } finally {
                    try {
                        ndef.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        throw new IllegalStateException("לא ניתן לקרוא מזהה תחנה מהתג");
    }

    private static String recordToText(NdefRecord record) {
        if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN
                && Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
            return new String(record.getPayload(), StandardCharsets.UTF_8);
        }
        if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN
                && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
            byte[] payload = record.getPayload();
            if (payload.length == 0) return "";
            int languageCodeLength = payload[0] & 0x3F;
            return new String(payload, 1 + languageCodeLength, payload.length - 1 - languageCodeLength, StandardCharsets.UTF_8);
        }
        return new String(record.getPayload(), StandardCharsets.UTF_8);
    }

    static String extractStationCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        // NDEF URI payload may include a URI identifier code byte prefix in some dumps
        if (value.contains("nfcattendance://")) {
            int idx = value.indexOf("nfcattendance://");
            value = value.substring(idx);
        }
        if (value.startsWith("nfcattendance://station/")) {
            Uri uri = Uri.parse(value);
            String last = uri.getLastPathSegment();
            return last != null ? last.toUpperCase() : null;
        }
        if (value.matches("(?i)GATE-[A-Z0-9-]+") || value.matches("(?i)[A-Z0-9-]{3,}")) {
            return value.toUpperCase();
        }
        return null;
    }
}
