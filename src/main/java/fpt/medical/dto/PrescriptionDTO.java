package fpt.medical.dto;

import lombok.Data;

@Data
public class PrescriptionDTO {

    private Long medicalRecordId;

    // Name of the medicine (a row is only saved when this field is filled in)
    private String medicineName;

    // How to take the medicine, for example "1 viên/lần, 2 lần/ngày"
    private String dosage;

    // Number of days to take the medicine.
    // We use Integer (not int) so an empty row can stay null instead of causing a binding error.
    private Integer durationDays;

    // Extra instructions, for example "uống sau khi ăn"
    private String instructions;
}
