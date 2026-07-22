package fpt.medical.service.impl;

import fpt.medical.dto.AdminDashboardStatsDTO;
import fpt.medical.entity.Department;
import fpt.medical.entity.WorkSchedule;
import fpt.medical.repository.DepartmentRepository;
import fpt.medical.repository.DoctorRepository;
import fpt.medical.repository.WorkScheduleRepository;
import fpt.medical.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkScheduleRepository workScheduleRepository;

    @Override
    public AdminDashboardStatsDTO getStats() {
        long totalDoctors = doctorRepository.count();
        long totalDepartments = departmentRepository.count();
        long totalSchedulesToday = workScheduleRepository.countByWorkDate(LocalDate.now());
        long doctorsWithoutDepartment = doctorRepository.countByDepartmentIsNull();
        long departmentsWithoutDoctors = departmentRepository.findDepartmentsWithoutDoctors().size();

        return AdminDashboardStatsDTO.builder()
                .totalDoctors(totalDoctors)
                .totalDepartments(totalDepartments)
                .totalSchedulesToday(totalSchedulesToday)
                .doctorsWithoutDepartment(doctorsWithoutDepartment)
                .departmentsWithoutDoctors(departmentsWithoutDoctors)
                .build();
    }

    @Override
    public List<WorkSchedule> getUpcomingSchedules(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return workScheduleRepository.findByWorkDateGreaterThanEqualOrderByWorkDateAsc(LocalDate.now(), pageable).getContent();
    }

    @Override
    public List<Department> getDepartmentsWithoutDoctors() {
        return departmentRepository.findDepartmentsWithoutDoctors();
    }

    @Override
    public List<fpt.medical.dto.DoctorCountByDepartmentDTO> getDoctorCountByDepartment() {
        return departmentRepository.countDoctorsPerDepartment();
    }

    @Override
    public java.util.Map<String, Long> getShiftDistribution() {
        List<WorkSchedule> allSchedules = workScheduleRepository.findAll();
        long morningCount = allSchedules.stream().filter(s -> s.getShift() == fpt.medical.enums.ShiftType.MORNING).count();
        long afternoonCount = allSchedules.stream().filter(s -> s.getShift() == fpt.medical.enums.ShiftType.AFTERNOON).count();

        java.util.Map<String, Long> distribution = new java.util.LinkedHashMap<>();
        distribution.put("Ca sáng", morningCount);
        distribution.put("Ca chiều", afternoonCount);
        return distribution;
    }
}
