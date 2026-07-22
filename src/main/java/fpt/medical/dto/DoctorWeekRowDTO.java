package fpt.medical.dto;

import fpt.medical.entity.Doctor;
import fpt.medical.entity.WorkSchedule;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorWeekRowDTO {
    private Doctor doctor;
    private Map<LocalDate, List<WorkSchedule>> schedulesByDate;
}
