package fpt.medical.service;

import fpt.medical.dto.AdminDashboardStatsDTO;
import fpt.medical.entity.Department;
import fpt.medical.entity.WorkSchedule;
import java.util.List;

public interface AdminDashboardService {
    AdminDashboardStatsDTO getStats();
    List<WorkSchedule> getUpcomingSchedules(int limit);
    List<Department> getDepartmentsWithoutDoctors();
}
