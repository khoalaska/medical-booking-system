package fpt.medical.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fpt.medical.dto.AdminDashboardStatsDTO;
import fpt.medical.dto.DoctorCountByDepartmentDTO;
import fpt.medical.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final ObjectMapper objectMapper;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AdminDashboardStatsDTO stats = adminDashboardService.getStats();
        model.addAttribute("stats", stats);
        model.addAttribute("upcomingSchedules", adminDashboardService.getUpcomingSchedules(8));
        model.addAttribute("departmentsWithoutDoctors", adminDashboardService.getDepartmentsWithoutDoctors());

        // Chart Data (convert to JSON string for safe JS parsing)
        try {
            List<DoctorCountByDepartmentDTO> deptCounts = adminDashboardService.getDoctorCountByDepartment();
            List<String> deptNames = deptCounts.stream().map(DoctorCountByDepartmentDTO::getDepartmentName).collect(java.util.stream.Collectors.toList());
            List<Long> doctorCounts = deptCounts.stream().map(DoctorCountByDepartmentDTO::getDoctorCount).collect(java.util.stream.Collectors.toList());
            Map<String, Long> shiftDist = adminDashboardService.getShiftDistribution();

            model.addAttribute("deptNamesJson", objectMapper.writeValueAsString(deptNames));
            model.addAttribute("doctorCountsJson", objectMapper.writeValueAsString(doctorCounts));
            model.addAttribute("shiftLabelsJson", objectMapper.writeValueAsString(new ArrayList<>(shiftDist.keySet())));
            model.addAttribute("shiftValuesJson", objectMapper.writeValueAsString(new ArrayList<>(shiftDist.values())));
        } catch (Exception e) {
            model.addAttribute("deptNamesJson", "[]");
            model.addAttribute("doctorCountsJson", "[]");
            model.addAttribute("shiftLabelsJson", "[]");
            model.addAttribute("shiftValuesJson", "[]");
        }

        model.addAttribute("activeMenu", "dashboard");
        return "admin/dashboard";
    }
}
