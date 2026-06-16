package fpt.medical.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AppointmentDTO {

    @NotNull
    private Long doctorId;

    @NotNull
    private LocalDate appointmentDate;

    @NotNull
    private LocalTime appointmentTime;

    private String notes;
}
