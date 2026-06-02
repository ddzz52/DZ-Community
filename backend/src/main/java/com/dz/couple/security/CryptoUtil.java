package com.dz.couple.security;

import com.dz.couple.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class CryptoUtil {
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public CryptoUtil(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
            byte[] out = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("encrypt_failed");
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            String[] parts = ciphertext.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("bad_ciphertext");
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] data = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
            byte[] out = cipher.doFinal(data);
            return new String(out, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("decrypt_failed");
        }
    }

    private SecretKeySpec getKey() {
        String secret = appProperties.getCrypto() == null ? null : appProperties.getCrypto().getSecret();
        if (secret == null || secret.trim().isEmpty()) {
            secret = appProperties.getJwt() == null ? "" : String.valueOf(appProperties.getJwt().getSecret());
        }
        byte[] key = Arrays.copyOf(sha256(secret.getBytes(StandardCharsets.UTF_8)), 16);
        return new SecretKeySpec(key, "AES");
    }

    private byte[] sha256(byte[] in) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(in);
        } catch (Exception e) {
            throw new IllegalStateException("sha256_failed");
        }
    }
}
