package com.example.bankapproval.security;

import com.example.bankapproval.domain.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class DemoPrincipal implements Serializable {
    private static final long serialVersionUID = 1L;
    private String userId;
    private String userName;
    private String departmentId;
    private UserRole role;
}
