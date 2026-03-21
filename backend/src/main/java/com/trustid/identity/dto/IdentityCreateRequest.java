package com.trustid.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdentityCreateRequest {
    @NotBlank
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String address;
    private String nationality;

    private String fatherName;
    private String motherName;
    private String occupation;
    private String maritalStatus;

    private String phone;
    private String alternatePhone;

    private String currentAddressLine1;
    private String currentAddressLine2;
    private String currentCity;
    private String currentDistrict;
    private String currentState;

    @Pattern(regexp = "^[A-Za-z0-9 -]{3,20}$", message = "Invalid pincode format")
    private String currentPincode;
    private String currentCountry;

    @Builder.Default
    private Boolean permanentSameAsCurrent = true;

    private String permanentAddressLine1;
    private String permanentAddressLine2;
    private String permanentCity;
    private String permanentDistrict;
    private String permanentState;
    private String permanentPincode;
    private String permanentCountry;
}
