package fpt.medical.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCountByDepartmentDTO {
    private String departmentName;
    private long doctorCount;
}
