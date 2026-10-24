package com.example.bankapproval.domain.approval;

import com.example.bankapproval.domain.model.TransactionApplication;
import com.example.bankapproval.security.DemoPrincipal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApprovalContext {
    private final TransactionApplication transaction;
    private final DemoPrincipal approver;
}
