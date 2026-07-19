package fpt.medical.dto;

import lombok.*;

@Getter
@Setter
@Builder
public class AdminDashboardStatsDTO {
    private long totalDoctors;
    private long totalDepartments;
    private long totalSchedulesToday;
    private long doctorsWithoutDepartment;
    private long departmentsWithoutDoctors;
}
