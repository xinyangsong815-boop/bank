package com.example.bankapproval.api.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.PositiveOrZero;

@Data
public class ApprovalRequest {
    @NotNull(message = "version 不能为空")
    @PositiveOrZero(message = "version 不能为负数")
    private Long version;
    @Size(max = 500, message = "审批意见最多 500 字")
    private String comment;
}
