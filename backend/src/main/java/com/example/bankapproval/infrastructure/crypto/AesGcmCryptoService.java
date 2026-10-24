package com.example.bankapproval.infrastructure.crypto;

import com.example.bankapproval.common.BizException;
import com.example.bankapproval.config.BankCryptoProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 同时提供机密性与完整性。每条数据生成独立 96 位随机 IV，存储格式为 Base64(iv + 密文 + tag)。
 * 教学项目从环境变量读密钥；真实银行系统应由 KMS/HSM 完成密钥轮换与解密审计。
 */
@Component
@ConditionalOnProperty(prefix = "bank.crypto", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AesGcmCryptoService implements CryptoService {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmCryptoService(BankCryptoProperties properties) {
        byte[] decoded = Base64.getDecoder().decode(properties.getBase64Key());
        if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
            throw new IllegalArgumentException("bank.crypto.base64-key 解码后必须为 16/24/32 字节");
        }
        this.key = new SecretKeySpec(decoded, "AES");
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception ex) {
            throw new BizException("ENCRYPTION_ERROR", "敏感数据加密失败");
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            byte[] packed = Base64.getDecoder().decode(ciphertext);
            if (packed.length <= IV_LENGTH) throw new IllegalArgumentException("密文过短");
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[packed.length - IV_LENGTH];
            System.arraycopy(packed, 0, iv, 0, IV_LENGTH);
            System.arraycopy(packed, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BizException("DECRYPTION_ERROR", "敏感数据解密失败或已被篡改");
        }
    }
}
