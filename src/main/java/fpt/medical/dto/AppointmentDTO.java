package fpt.medical.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentDTO {

    @NotNull
    private Long timeSlotId;

    private String notes;
}
