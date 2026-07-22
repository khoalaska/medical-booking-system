package fpt.medical.service;

import fpt.medical.dto.AdminDashboardStatsDTO;
import fpt.medical.dto.DoctorCountByDepartmentDTO;
import fpt.medical.entity.Department;
import fpt.medical.entity.WorkSchedule;
import java.util.List;
import java.util.Map;

public interface AdminDashboardService {
    AdminDashboardStatsDTO getStats();
    List<WorkSchedule> getUpcomingSchedules(int limit);
    List<Department> getDepartmentsWithoutDoctors();
    List<DoctorCountByDepartmentDTO> getDoctorCountByDepartment();
    Map<String, Long> getShiftDistribution();
}
