package com.gangwon.companion.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Component
public class PersonalDataCrypto {
    private final byte[] key;
    public PersonalDataCrypto(@Value("${personal-data.encryption-key}") String key) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
        if (this.key.length < 32) throw new IllegalArgumentException("personal-data.encryption-key must be at least 32 bytes");
    }
    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.trim().toLowerCase().getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) { throw new IllegalStateException("Unable to hash personal data", e); }
    }
}
