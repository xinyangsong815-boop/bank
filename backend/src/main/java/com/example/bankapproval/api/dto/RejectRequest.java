package com.example.bankapproval.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.PositiveOrZero;

@Data
public class RejectRequest {
    @NotNull(message = "version 不能为空")
    @PositiveOrZero(message = "version 不能为负数")
    private Long version;
    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 500, message = "驳回原因最多 500 字")
    private String reason;
}
