package com.example.bankapproval.api;

import com.example.bankapproval.api.dto.*;
import com.example.bankapproval.application.TransactionService;
import com.example.bankapproval.common.ApiResponse;
import com.example.bankapproval.common.PageResult;
import com.example.bankapproval.domain.model.ApprovalRecord;
import com.example.bankapproval.domain.model.AuditLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * @RestController 直接返回 JSON；@Validated 让路径和查询参数也参与校验。
 */
@RestController
@RequestMapping("/api/transactions")
@Validated
public class TransactionController {
    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.create(body, clientIp(request)));
    }

    @GetMapping
    public ApiResponse<PageResult<TransactionResponse>> list(@Valid @ModelAttribute TransactionQueryRequest query) {
        return ApiResponse.ok(service.list(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> get(@PathVariable @Positive Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<TransactionResponse> submit(@PathVariable @Positive Long id,
                                                   @Valid @RequestBody SubmitRequest body,
                                                   HttpServletRequest request) {
        return ApiResponse.ok(service.submit(id, body, clientIp(request)));
    }

    @PostMapping("/{id}/resubmit")
    public ApiResponse<TransactionResponse> resubmit(@PathVariable @Positive Long id,
                                                     @Valid @RequestBody ResubmitTransactionRequest body,
                                                     HttpServletRequest request) {
        return ApiResponse.ok(service.resubmit(id, body, clientIp(request)));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<TransactionResponse> approve(@PathVariable @Positive Long id,
                                                    @Valid @RequestBody ApprovalRequest body,
                                                    HttpServletRequest request) {
        return ApiResponse.ok(service.approve(id, body, clientIp(request)));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<TransactionResponse> reject(@PathVariable @Positive Long id,
                                                   @Valid @RequestBody RejectRequest body,
                                                   HttpServletRequest request) {
        return ApiResponse.ok(service.reject(id, body, clientIp(request)));
    }

    @GetMapping("/{id}/approvals")
    public ApiResponse<List<ApprovalRecord>> approvals(@PathVariable @Positive Long id) {
        return ApiResponse.ok(service.approvalHistory(id));
    }

    @GetMapping("/{id}/audits")
    public ApiResponse<List<AuditLog>> audits(@PathVariable @Positive Long id) {
        return ApiResponse.ok(service.auditHistory(id));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.trim().isEmpty() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
