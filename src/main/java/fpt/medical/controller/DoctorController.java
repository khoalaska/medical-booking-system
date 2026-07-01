package fpt.medical.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activeMenu", "dashboard");
        return "doctor/dashboard";
    }

    @GetMapping("/schedule")
    public String schedule(Model model) {
        model.addAttribute("activeMenu", "schedule");
        return "doctor/schedule";
    }

    @GetMapping("/patient-history")
    public String patientHistory(Model model) {
        model.addAttribute("activeMenu", "patient-history");
        return "doctor/patient-history";
    }
}
