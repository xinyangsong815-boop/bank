package com.example.bankapproval.security;

import com.example.bankapproval.config.BankSecurityProperties;
import com.example.bankapproval.domain.model.UserRole;
import com.example.bankapproval.common.ApiResponse;
import com.example.bankapproval.common.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * 教学用请求头认证适配器。
 * <p>它信任客户端传来的身份，因此绝对不能用于生产环境；生产应替换为 OAuth2/OIDC、双因素认证和银行 IAM。</p>
 */
public class DemoHeaderAuthenticationFilter extends OncePerRequestFilter {
    private final BankSecurityProperties properties;
    private final ObjectMapper objectMapper;

    public DemoHeaderAuthenticationFilter(BankSecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (properties.isDemoAuthEnabled() && SecurityContextHolder.getContext().getAuthentication() == null) {
            String userId = request.getHeader("X-User-Id");
            String departmentId = request.getHeader("X-Dept-Id");
            String roleHeader = request.getHeader("X-Role");
            if (userId == null || departmentId == null || roleHeader == null) {
                writeError(response, 401, "UNAUTHENTICATED", "请显式选择演示身份并发送身份请求头");
                return;
            }
            if (!userId.matches("[A-Za-z0-9_-]{1,32}") || !departmentId.matches("[A-Za-z0-9_-]{1,32}")) {
                writeError(response, 400, "INVALID_IDENTITY", "演示用户号或部门号格式不正确");
                return;
            }
            UserRole role;
            try { role = UserRole.parse(roleHeader); }
            catch (BizException ex) {
                // Filter 位于 MVC 之外，异常不会到达 @RestControllerAdvice，必须自行输出 JSON。
                writeError(response, 400, "INVALID_ROLE", "未知演示角色");
                return;
            }
            String userName = defaultIfBlank(request.getHeader("X-User-Name"), userId);
            if (userName.length() > 64) {
                writeError(response, 400, "INVALID_IDENTITY", "演示用户名最多 64 个字符");
                return;
            }
            DemoPrincipal principal = new DemoPrincipal(userId, userName, departmentId, role);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), new ApiResponse<Void>(false, code, message, null));
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
