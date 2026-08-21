package com.respondr.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacUtilsTest {

    private static final String SECRET = "my-secret-key-123";
    private static final String PAYLOAD = "{\"title\":\"Server High CPU\",\"severity\":\"CRITICAL\"}";

    @Test
    void calculatesHmacSignature() {
        String signature = HmacUtils.calculateHmacSha256(PAYLOAD, SECRET);
        assertThat(signature).isNotBlank().hasSize(64);
    }

    @Test
    void verifiesValidSignature() {
        String signature = HmacUtils.calculateHmacSha256(PAYLOAD, SECRET);
        assertThat(HmacUtils.verifyHmacSha256(PAYLOAD, SECRET, signature)).isTrue();
        assertThat(HmacUtils.verifyHmacSha256(PAYLOAD, SECRET, "sha256=" + signature)).isTrue();
    }

    @Test
    void rejectsInvalidSignature() {
        assertThat(HmacUtils.verifyHmacSha256(PAYLOAD, SECRET, "invalid-signature")).isFalse();
    }

    @Test
    void rejectsMissingSignature() {
        assertThat(HmacUtils.verifyHmacSha256(PAYLOAD, SECRET, null)).isFalse();
        assertThat(HmacUtils.verifyHmacSha256(PAYLOAD, SECRET, "")).isFalse();
    }
}
