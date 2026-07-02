package fpt.medical.controller;
import fpt.medical.entity.Appointment;
import fpt.medical.entity.Doctor;
import fpt.medical.security.CustomUserDetails;
import fpt.medical.service.AppointmentService;
import fpt.medical.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
@Controller
@RequestMapping("/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    @GetMapping("/dashboard")
    public String dashboard( @AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model ) {

        Doctor doctor = doctorService.getByUserId(userDetails.getUser().getId());

        Long doctorId = doctor.getId();
        long todayCount   = appointmentService.countTodayAppointments(doctorId);
        long pendingCount = appointmentService.countPendingToday(doctorId);
        long completedCount = appointmentService.countCompletedToday(doctorId);
        List<Appointment> appointments = appointmentService.getTodayAppointments(doctorId);


        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("doctorName", doctor.getUser().getFullName());
        model.addAttribute("todayAppointments", todayCount);
        model.addAttribute("pendingPatients", pendingCount);
        model.addAttribute("completedToday", completedCount);
        model.addAttribute("appointments", appointments);


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
