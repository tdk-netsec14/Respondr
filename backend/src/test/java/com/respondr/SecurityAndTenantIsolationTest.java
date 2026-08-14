package com.respondr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.respondr.auth.dto.LoginRequest;
import com.respondr.auth.dto.RegisterRequest;
import com.respondr.servicecatalog.dto.CreateServiceRequest;
import com.respondr.team.dto.CreateTeamRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityAndTenantIsolationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void rejectsUnauthenticatedRequestWith401() throws Exception {
        mockMvc.perform(get("/api/v1/organizations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidTokenWith401() throws Exception {
        mockMvc.perform(get("/api/v1/organizations")
                        .header("Authorization", "Bearer invalid-jwt-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerAndLoginFlowWorks() throws Exception {
        long nano = System.nanoTime();
        String email = "admin-" + nano + "@acme.com";
        String slug = "acme-" + nano;

        RegisterRequest registerReq = new RegisterRequest(
                email, "Secret123!", "Admin User", "Acme Corp", slug);

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.organization.slug").value(slug))
                .andReturn();

        String regResponseBody = regResult.getResponse().getContentAsString();
        Map<?, ?> regResponse = objectMapper.readValue(regResponseBody, Map.class);
        String token = (String) regResponse.get("accessToken");

        // Use token to access own org
        mockMvc.perform(get("/api/v1/organizations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value(slug));

        // Test login endpoint
        LoginRequest loginReq = new LoginRequest(email, "Secret123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void enforcesCrossTenantAccessDenial() throws Exception {
        long nanoA = System.nanoTime();
        String emailA = "userA-" + nanoA + "@orga.com";
        String slugA = "orga-" + nanoA;

        RegisterRequest regA = new RegisterRequest(emailA, "Pass123!", "User A", "Org A", slugA);
        MvcResult resA = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regA)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> mapA = objectMapper.readValue(resA.getResponse().getContentAsString(), Map.class);
        String tokenA = (String) mapA.get("accessToken");
        Map<?, ?> orgA = (Map<?, ?>) mapA.get("organization");
        UUID orgIdA = UUID.fromString((String) orgA.get("id"));

        long nanoB = System.nanoTime() + 1;
        String emailB = "userB-" + nanoB + "@orgb.com";
        String slugB = "orgb-" + nanoB;

        RegisterRequest regB = new RegisterRequest(emailB, "Pass123!", "User B", "Org B", slugB);
        MvcResult resB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regB)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> mapB = objectMapper.readValue(resB.getResponse().getContentAsString(), Map.class);
        String tokenB = (String) mapB.get("accessToken");
        Map<?, ?> orgB = (Map<?, ?>) mapB.get("organization");
        UUID orgIdB = UUID.fromString((String) orgB.get("id"));

        // User A creates a team in Org A -> Success
        CreateTeamRequest teamReqA = new CreateTeamRequest("Platform A", "Org A Team");
        MvcResult teamResA = mockMvc.perform(post("/api/v1/organizations/" + orgIdA + "/teams")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teamReqA)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> teamObjA = objectMapper.readValue(teamResA.getResponse().getContentAsString(), Map.class);
        UUID teamIdA = UUID.fromString((String) teamObjA.get("id"));

        // Cross-tenant attack 1: User B tries to list teams of Org A using Org B token -> 403 Forbidden
        mockMvc.perform(get("/api/v1/organizations/" + orgIdA + "/teams")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // Cross-tenant attack 2: User B tries to read team A directly by ID -> 403 Forbidden
        mockMvc.perform(get("/api/v1/teams/" + teamIdA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // Cross-tenant attack 3: User B tries to create a service in Org A -> 403 Forbidden
        CreateServiceRequest svcReq = new CreateServiceRequest(null, "Hacked Service", "hacked-svc", "desc");
        mockMvc.perform(post("/api/v1/organizations/" + orgIdA + "/services")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(svcReq)))
                .andExpect(status().isForbidden());
    }
}
