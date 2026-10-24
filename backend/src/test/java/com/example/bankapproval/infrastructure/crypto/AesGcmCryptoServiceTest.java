package com.example.bankapproval.infrastructure.crypto;

import com.example.bankapproval.config.BankCryptoProperties;
import com.example.bankapproval.common.BizException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmCryptoServiceTest {
    @Test
    void roundTripAndRandomIv() {
        BankCryptoProperties properties = new BankCryptoProperties();
        properties.setBase64Key("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        AesGcmCryptoService service = new AesGcmCryptoService(properties);
        String first = service.encrypt("6222021234567890");
        String second = service.encrypt("6222021234567890");
        assertNotEquals(first, second, "随机 IV 应使相同明文产生不同密文");
        assertEquals("6222021234567890", service.decrypt(first));
        assertFalse(first.contains("622202"));
        byte[] tampered = Base64.getDecoder().decode(first);
        tampered[tampered.length - 1] ^= 1;
        BizException error = assertThrows(BizException.class,
                () -> service.decrypt(Base64.getEncoder().encodeToString(tampered)));
        assertEquals("DECRYPTION_ERROR", error.getCode(), "认证标签必须阻止篡改后的密文被使用");
    }

    @Test
    void shortAccountNeverExposesAllDigits() {
        assertEquals("**** 5678", SensitiveMasker.account("12345678"));
        assertEquals("6222 **** **** 7890", SensitiveMasker.account("6222021234567890"));
    }
}
