package com.example.bankapproval.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final String code;
    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }
}
