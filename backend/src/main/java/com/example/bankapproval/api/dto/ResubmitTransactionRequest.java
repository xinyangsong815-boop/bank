package com.example.bankapproval.api.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class ResubmitTransactionRequest {
    @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") private String mobile;
    @NotBlank @Pattern(regexp = "^(\\d{15}|\\d{17}[0-9Xx])$") private String idCard;
    @NotBlank @Pattern(regexp = "^\\d{8,32}$") private String account;
    @NotBlank @Pattern(regexp = "^\\d{8,32}$") private String counterpartyAccount;
    @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) private BigDecimal amount;
    @NotBlank @Pattern(regexp = "^[A-Z]{3}$") private String currency;
    @NotBlank @Size(max = 500) private String purpose;
    @NotNull @PositiveOrZero private Long version;
}
