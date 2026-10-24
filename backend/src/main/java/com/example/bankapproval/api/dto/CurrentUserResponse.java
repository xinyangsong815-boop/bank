package com.example.bankapproval.api.dto;

import com.example.bankapproval.security.DemoPrincipal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUserResponse {
    private String userId;
    private String userName;
    private String departmentId;
    private String role;
    private String securityWarning;

    public static CurrentUserResponse from(DemoPrincipal p) {
        return new CurrentUserResponse(p.getUserId(), p.getUserName(), p.getDepartmentId(), p.getRole().name(),
                "请求头认证仅供教学演示，禁止用于生产环境");
    }
}
