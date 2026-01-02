package com.kevdev.iam.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevdev.iam.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RefreshFlowIT extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void refreshRotatesAndOldRefreshCannotBeReusedAndRbacStillWorks() throws Exception {
        Tokens admin = login("demo", "admin", "Admin123!");
        Tokens adminRefreshed = refresh("demo", admin.refreshToken);

        mockMvc.perform(post("/auth/refresh")
                .header("X-Tenant-Key", "demo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + admin.refreshToken + "\"}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/ping")
                .header("X-Tenant-Key", "demo")
                .header("Authorization", "Bearer " + adminRefreshed.accessToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/secure/ping")
                .header("X-Tenant-Key", "demo")
                .header("Authorization", "Bearer " + adminRefreshed.accessToken))
            .andExpect(status().isOk());

        Tokens user = login("demo", "user", "User123!");
        mockMvc.perform(get("/secure/ping")
                .header("X-Tenant-Key", "demo")
                .header("Authorization", "Bearer " + user.accessToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/admin/ping")
                .header("X-Tenant-Key", "demo")
                .header("Authorization", "Bearer " + user.accessToken))
            .andExpect(status().isForbidden());
    }

    private Tokens login(String tenantKey, String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

        String res = mockMvc.perform(post("/auth/login")
                .header("X-Tenant-Key", tenantKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode json = objectMapper.readTree(res);
        return new Tokens(json.get("accessToken").asText(), json.get("refreshToken").asText());
    }

    private Tokens refresh(String tenantKey, String refreshToken) throws Exception {
        String body = "{\"refreshToken\":\"" + refreshToken + "\"}";

        String res = mockMvc.perform(post("/auth/refresh")
                .header("X-Tenant-Key", tenantKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode json = objectMapper.readTree(res);
        return new Tokens(json.get("accessToken").asText(), json.get("refreshToken").asText());
    }

    private record Tokens(String accessToken, String refreshToken) {}
}

