package fpt.medical.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MedicalRecordDTO {

    private Long appointmentId;

    // Diagnosis is required, so we validate it with @NotBlank
    @NotBlank
    private String diagnosis;

    private String treatment;

    private String notes;

    // The list of prescription rows shown on the diagnosis screen.
    // We do NOT annotate it with @Valid, so blank rows are allowed and simply skipped when saving.
    private List<PrescriptionDTO> prescriptions = new ArrayList<>();
}
