package com.respondr.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.respondr.common.exception.ConflictException;
import com.respondr.common.exception.ResourceNotFoundException;
import com.respondr.common.security.JwtService;
import com.respondr.organization.dto.CreateOrganizationRequest;
import com.respondr.organization.dto.OrganizationResponse;
import com.respondr.organization.dto.UpdateOrganizationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class OrganizationControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @MockBean OrganizationService organizationService;
    @MockBean JwtService jwtService;

    private static final OrganizationResponse SAMPLE = new OrganizationResponse(
            UUID.randomUUID(), "Acme", "acme", OffsetDateTime.now(), OffsetDateTime.now());

    // ── GET /api/v1/organizations ─────────────────────────────────────────────

    @Test
    void list_returns200WithPage() throws Exception {
        Page<OrganizationResponse> page = new PageImpl<>(List.of(SAMPLE));
        when(organizationService.listOrganizations(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug", is("acme")));
    }

    // ── GET /api/v1/organizations/{id} ────────────────────────────────────────

    @Test
    void getById_returns200() throws Exception {
        when(organizationService.getById(SAMPLE.id())).thenReturn(SAMPLE);

        mockMvc.perform(get("/api/v1/organizations/" + SAMPLE.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Acme")));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        UUID missing = UUID.randomUUID();
        when(organizationService.getById(missing))
                .thenThrow(ResourceNotFoundException.organization(missing.toString()));

        mockMvc.perform(get("/api/v1/organizations/" + missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code",    is("ORGANIZATION_NOT_FOUND")))
                .andExpect(jsonPath("$.status",  is(404)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path",    notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // ── POST /api/v1/organizations ────────────────────────────────────────────

    @Test
    void create_returns201() throws Exception {
        when(organizationService.create(any())).thenReturn(SAMPLE);

        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateOrganizationRequest("Acme", "acme"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug", is("acme")));
    }

    @Test
    void create_returns400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateOrganizationRequest("", "valid-slug"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code",   is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void create_returns400_whenSlugInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateOrganizationRequest("Acme", "INVALID SLUG!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void create_returns409_whenSlugTaken() throws Exception {
        when(organizationService.create(any())).thenThrow(ConflictException.slugTaken("acme"));

        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateOrganizationRequest("Acme", "acme"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("ORGANIZATION_SLUG_TAKEN")));
    }

    // ── PATCH /api/v1/organizations/{id} ──────────────────────────────────────

    @Test
    void update_returns200() throws Exception {
        when(organizationService.update(any(), any())).thenReturn(SAMPLE);

        mockMvc.perform(patch("/api/v1/organizations/" + SAMPLE.id())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateOrganizationRequest("Acme Updated"))))
                .andExpect(status().isOk());
    }

    // ── DELETE /api/v1/organizations/{id} ─────────────────────────────────────

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(organizationService).delete(any());

        mockMvc.perform(delete("/api/v1/organizations/" + SAMPLE.id()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404_whenNotFound() throws Exception {
        UUID missing = UUID.randomUUID();
        doThrow(ResourceNotFoundException.organization(missing.toString()))
                .when(organizationService).delete(missing);

        mockMvc.perform(delete("/api/v1/organizations/" + missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("ORGANIZATION_NOT_FOUND")));
    }
}
