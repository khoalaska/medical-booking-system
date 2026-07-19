package fpt.medical.controller;

import fpt.medical.dto.AdminDashboardStatsDTO;
import fpt.medical.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AdminDashboardStatsDTO stats = adminDashboardService.getStats();
        model.addAttribute("stats", stats);
        model.addAttribute("upcomingSchedules", adminDashboardService.getUpcomingSchedules(8));
        model.addAttribute("departmentsWithoutDoctors", adminDashboardService.getDepartmentsWithoutDoctors());
        model.addAttribute("activeMenu", "dashboard");
        return "admin/dashboard";
    }
}
