package com.example.bankapproval.infrastructure.crypto;

public interface CryptoService {
    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
