package com.example.bankapproval.api;

import com.example.bankapproval.api.dto.CurrentUserResponse;
import com.example.bankapproval.common.ApiResponse;
import com.example.bankapproval.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SessionController {
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.ok(CurrentUserResponse.from(SecurityUtils.currentUser()));
    }
}
