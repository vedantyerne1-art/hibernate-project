package com.trustid.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class VerificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private VerificationService verificationService;

    @Test
    @WithMockUser(username = "user@trustid.local", roles = {"USER"})
    void userCannotAccessAdminRequests() throws Exception {
        mockMvc.perform(get("/api/verification/admin/requests"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Access Denied"));
    }

    @Test
    @WithMockUser(username = "admin@trustid.local", roles = {"ADMIN"})
    void adminCanFetchRequests() throws Exception {
        VerificationRequestDTO dto = VerificationRequestDTO.builder()
                .id(7L)
                .status("PENDING")
                .submittedAt(LocalDateTime.now())
                .user(VerificationRequestDTO.UserSummary.builder()
                        .id(10L)
                        .fullName("Test User")
                        .email("user@trustid.local")
                        .build())
                .identityProfile(VerificationRequestDTO.IdentitySummary.builder()
                        .id(20L)
                        .fullName("Test User")
                        .identityNumber("ID-1")
                        .dob(LocalDate.of(2000, 1, 1))
                        .status("UNDER_REVIEW")
                        .build())
                .documents(List.of())
                .build();

        when(verificationService.getRequestsByStatusDto(eq(VerificationRequest.VerificationStatus.PENDING)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/verification/admin/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "user@trustid.local", roles = {"USER"})
    void userCannotReviewRequest() throws Exception {
        ReviewVerificationRequest request = new ReviewVerificationRequest();
        request.setStatus(VerificationRequest.VerificationStatus.APPROVED);
        request.setNotes("ok");

        mockMvc.perform(post("/api/verification/admin/review/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access Denied"));
    }

    @Test
    @WithMockUser(username = "admin@trustid.local", roles = {"ADMIN"})
    void adminCanReviewRequest() throws Exception {
        ReviewVerificationRequest request = new ReviewVerificationRequest();
        request.setStatus(VerificationRequest.VerificationStatus.APPROVED);
        request.setNotes("approved");

        VerificationRequestDTO response = VerificationRequestDTO.builder()
                .id(1L)
                .status("APPROVED")
                .notes("approved")
                .submittedAt(LocalDateTime.now())
                .reviewedAt(LocalDateTime.now())
                .user(VerificationRequestDTO.UserSummary.builder().id(10L).fullName("U").email("u@x.com").build())
                .identityProfile(VerificationRequestDTO.IdentitySummary.builder()
                        .id(20L)
                        .fullName("U")
                        .identityNumber("ID-1")
                        .dob(LocalDate.of(2000, 1, 1))
                        .status("APPROVED")
                        .build())
                .documents(List.of())
                .build();

        when(verificationService.reviewRequestDto(eq("admin@trustid.local"), eq(1L), org.mockito.ArgumentMatchers.any(ReviewVerificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/verification/admin/review/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(verificationService).reviewRequestDto(eq("admin@trustid.local"), eq(1L), org.mockito.ArgumentMatchers.any(ReviewVerificationRequest.class));
    }
}
