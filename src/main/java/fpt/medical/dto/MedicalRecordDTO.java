package fpt.medical.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MedicalRecordDTO {

    private Long appointmentId;

    @NotBlank
    private String diagnosis;

    private String notes;
    private String treatment;
}
