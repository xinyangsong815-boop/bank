package com.example.bankapproval.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 从 HTTP 入口验证真实过滤器、方法权限、参数绑定、事务服务与 MyBatis XML。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransactionApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private static final String BODY = "{\"mobile\":\"13800138000\",\"idCard\":\"110101199001011234\","
            + "\"account\":\"6222021234567890\",\"counterpartyAccount\":\"6217001234567890\","
            + "\"amount\":\"9999999999999999.99\",\"currency\":\"CNY\",\"purpose\":\"设备采购\"}";

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, String user, String role, String dept) {
        return request.header("X-User-Id", user).header("X-Role", role).header("X-Dept-Id", dept);
    }

    private long createDraft() throws Exception {
        String response = mvc.perform(as(post("/api/transactions").contentType("application/json").content(BODY),
                        "applicant01", "APPLICANT", "D001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value("9999999999999999.99"))
                .andExpect(jsonPath("$.data.mobileMasked", not("13800138000")))
                .andExpect(jsonPath("$.data.mobileCiphertext").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("id").asLong();
    }

    private void submit(long id) throws Exception {
        mvc.perform(as(post("/api/transactions/{id}/submit", id).contentType("application/json").content("{\"version\":0}"),
                        "applicant01", "APPLICANT", "D001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PENDING_L1"));
    }

    @Test
    void completeHttpFlowAndStaleVersionAreConsistent() throws Exception {
        long id = createDraft();
        submit(id);
        for (int level = 1; level <= 4; level++) {
            mvc.perform(as(post("/api/transactions/{id}/approve", id).contentType("application/json")
                            .content("{\"version\":" + level + ",\"comment\":\"同意\"}"),
                            "approver0" + level, "APPROVER_L" + level, "D001"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(level + 1));
            if (level == 1) {
                mvc.perform(as(post("/api/transactions/{id}/approve", id).contentType("application/json")
                                .content("{\"version\":1}"), "approver01", "APPROVER_L1", "D001"))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
            }
        }
        mvc.perform(as(get("/api/transactions/{id}", id), "auditor01", "AUDITOR", "AUDIT"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.currentLevel").value(0));
        mvc.perform(as(get("/api/transactions/{id}/approvals", id), "auditor01", "AUDITOR", "AUDIT"))
                .andExpect(jsonPath("$.data", hasSize(4)));
        mvc.perform(as(get("/api/transactions/{id}/audits", id), "auditor01", "AUDITOR", "AUDIT"))
                .andExpect(jsonPath("$.data", hasSize(6)));
    }

    @Test
    void roleOwnerDepartmentAndSeparationAreEnforced() throws Exception {
        long id = createDraft();
        mvc.perform(as(get("/api/transactions/{id}", id), "other", "APPLICANT", "D001"))
                .andExpect(status().isForbidden());
        mvc.perform(as(get("/api/transactions"), "other", "APPLICANT", "D001"))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(as(post("/api/transactions").contentType("application/json").content(BODY),
                        "auditor01", "AUDITOR", "AUDIT")).andExpect(status().isForbidden());
        submit(id);
        mvc.perform(as(post("/api/transactions/{id}/approve", id).contentType("application/json").content("{\"version\":1}"),
                        "approver01", "APPROVER_L1", "D002")).andExpect(status().isForbidden());
        mvc.perform(as(post("/api/transactions/{id}/approve", id).contentType("application/json").content("{\"version\":1}"),
                        "applicant01", "APPROVER_L1", "D001")).andExpect(status().isForbidden());
        mvc.perform(as(post("/api/transactions/{id}/approve", id).contentType("application/json").content("{\"version\":1}"),
                        "approver02", "APPROVER_L2", "D001")).andExpect(status().isBadRequest());
        mvc.perform(as(get("/api/transactions/{id}/approvals", id), "auditor01", "AUDITOR", "AUDIT"))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void bodyQueryPathAndJsonErrorsReturn400() throws Exception {
        mvc.perform(as(get("/api/transactions?page=0"), "applicant01", "APPLICANT", "D001"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(as(get("/api/transactions?status=UNKNOWN"), "applicant01", "APPLICANT", "D001"))
                .andExpect(status().isBadRequest());
        mvc.perform(as(get("/api/transactions/-1"), "applicant01", "APPLICANT", "D001"))
                .andExpect(status().isBadRequest());
        mvc.perform(as(get("/api/transactions?startDate=not-a-date"), "applicant01", "APPLICANT", "D001"))
                .andExpect(status().isBadRequest());
        mvc.perform(as(post("/api/transactions").contentType("application/json").content("{}"),
                        "applicant01", "APPLICANT", "D001"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.mobile").exists());
        mvc.perform(as(post("/api/transactions").contentType("application/json").content("{"),
                        "applicant01", "APPLICANT", "D001")).andExpect(status().isBadRequest());
    }

    @Test
    void filterErrorsAreJsonAndExplicitIdentityIsRequired() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(as(get("/api/me"), "applicant01", "UNKNOWN", "D001"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ROLE"));
    }
}
