package fpt.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PrescriptionDTO {

    private Long medicalRecordId;

    @NotBlank
    private String medicineName;

    @NotBlank
    private String dosage;

    @Positive
    private int durationDays;

    private String instructions;
}
