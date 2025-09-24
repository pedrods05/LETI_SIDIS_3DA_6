package com.pcm.psoft.pcmclinic_api.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhotoDTO {
    @NotBlank(message = "URL is required")
    private String url;

    @NotNull(message = "Upload date is required")
    private LocalDateTime uploadedAt;
} 