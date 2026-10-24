package com.example.bankapproval.infrastructure.crypto;

public final class SensitiveMasker {
    private SensitiveMasker() { }

    public static String mobile(String value) {
        if (value == null || value.length() < 7) return "***";
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    public static String idCard(String value) {
        if (value == null || value.length() < 8) return "***";
        return value.substring(0, 4) + "**********" + value.substring(value.length() - 4);
    }

    public static String account(String value) {
        if (value == null || value.length() < 8) return "***";
        // 短账户不能同时展示前四后四，否则 8 位账户的全部数字都会泄露。
        return (value.length() >= 12 ? value.substring(0, 4) + " **** **** " : "**** ")
                + value.substring(value.length() - 4);
    }
}
