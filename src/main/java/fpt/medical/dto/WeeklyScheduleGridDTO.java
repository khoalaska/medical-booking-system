package fpt.medical.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyScheduleGridDTO {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private List<LocalDate> days;
    private List<DoctorWeekRowDTO> rows;
}
