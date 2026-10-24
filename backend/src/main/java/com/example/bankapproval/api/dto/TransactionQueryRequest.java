package com.example.bankapproval.api.dto;

import com.example.bankapproval.domain.model.TransactionStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class TransactionQueryRequest {
    @Size(max = 100) private String keyword;
    private TransactionStatus status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate endDate;
    @Min(1) @Max(1000000) private int page = 1;
    @Min(1) @Max(100) private int size = 10;
}
