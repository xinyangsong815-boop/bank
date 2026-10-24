package com.example.bankapproval.infrastructure.crypto;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 仅用于自动化测试或本地排错。生产环境不得关闭字段加密。 */
@Component
@ConditionalOnProperty(prefix = "bank.crypto", name = "enabled", havingValue = "false")
public class PlaintextCryptoService implements CryptoService {
    @Override public String encrypt(String plaintext) { return plaintext; }
    @Override public String decrypt(String ciphertext) { return ciphertext; }
}
