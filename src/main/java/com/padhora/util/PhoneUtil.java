package com.padhora.util;

// Normalizes phone numbers to E.164 (+91XXXXXXXXXX) so we don't have to migrate bare
// 10-digit strings later when tutors/parents outside India show up.
// Default country code is India-only for now (matches the rest of the product) -
// swap DEFAULT_COUNTRY_CODE or add a country param when that changes.
public final class PhoneUtil {

    private static final String DEFAULT_COUNTRY_CODE = "91";

    private PhoneUtil() {}

    public static String toE164(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;
        if (raw.trim().startsWith("+")) return "+" + digits;
        if (digits.length() == 10) return "+" + DEFAULT_COUNTRY_CODE + digits;
        // Already has a country code prefix (e.g. "919876543210") or is some other format -
        // pass through with a leading + rather than guessing wrong.
        return "+" + digits;
    }
}
