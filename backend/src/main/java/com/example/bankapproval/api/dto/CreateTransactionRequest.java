package com.example.bankapproval.api.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class CreateTransactionRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^(\\d{15}|\\d{17}[0-9Xx])$", message = "身份证号格式不正确")
    private String idCard;

    @NotBlank(message = "付款账户不能为空")
    @Pattern(regexp = "^\\d{8,32}$", message = "付款账户应为 8-32 位数字")
    private String account;

    @NotBlank(message = "收款账户不能为空")
    @Pattern(regexp = "^\\d{8,32}$", message = "收款账户应为 8-32 位数字")
    private String counterpartyAccount;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    @Digits(integer = 16, fraction = 2, message = "金额最多 16 位整数、2 位小数")
    private BigDecimal amount;

    @NotBlank(message = "币种不能为空")
    @Pattern(regexp = "^[A-Z]{3}$", message = "币种应为三位大写代码")
    private String currency;

    @NotBlank(message = "交易用途不能为空")
    @Size(max = 500, message = "交易用途最多 500 字")
    private String purpose;
}
