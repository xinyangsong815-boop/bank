package com.example.bankapproval.api.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@Data
public class SubmitRequest {
    @NotNull(message = "version 不能为空")
    @PositiveOrZero(message = "version 不能为负数")
    private Long version;
}
