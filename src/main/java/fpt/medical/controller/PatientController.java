package fpt.medical.controller;

import fpt.medical.entity.Department;
import fpt.medical.entity.Doctor;
import fpt.medical.entity.TimeSlot;
import fpt.medical.enums.ShiftType;
import fpt.medical.service.DepartmentService;
import fpt.medical.service.DoctorService;
import fpt.medical.service.WorkScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final DepartmentService departmentService;
    private final DoctorService doctorService;
    private final WorkScheduleService workScheduleService;

    @GetMapping("/book-appointment")
    public String bookAppointment(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "appointmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
            @RequestParam(value = "shift", required = false) ShiftType shift,
            @RequestParam(value = "timeSlotId", required = false) Long timeSlotId,
            Model model) {

        Page<Department> departmentPage = departmentService.getDepartments(null, 0, 15, "name", "asc");
        model.addAttribute("departments", departmentPage.getContent());

        if (departmentId != null) {
            Department selectedDepartment = departmentService.getById(departmentId);
            model.addAttribute("selectedDepartment", selectedDepartment);
            model.addAttribute("doctors", doctorService.getDoctorsByDepartment(departmentId));
        }

        if (doctorId != null) {
            Doctor selectedDoctor = doctorService.getById(doctorId);

            model.addAttribute("selectedDoctor", selectedDoctor);
            model.addAttribute("availableDates", workScheduleService.getAvailableDates(doctorId));
            model.addAttribute("minDate", workScheduleService.getMinAvailableDate(doctorId));
            model.addAttribute("maxDate", workScheduleService.getMaxAvailableDate(doctorId));
        }

        if (doctorId != null && appointmentDate != null) {
            model.addAttribute("appointmentDate", appointmentDate);
            model.addAttribute("availableShifts", workScheduleService.getSchedulesByDate(doctorId, appointmentDate));
        }

        if (doctorId != null && appointmentDate != null && shift != null) {
            model.addAttribute("selectedShift", shift);
            model.addAttribute("availableTimeSlots", workScheduleService.getAvailableTimeSlots(doctorId, appointmentDate, shift));
        }

        if (timeSlotId != null) {
            TimeSlot selectedTimeSlot = workScheduleService.getTimeSlotById(timeSlotId);
            model.addAttribute("selectedTimeSlot", selectedTimeSlot);
        }

        return "patient/book-appointment";
    }

    @GetMapping("/doctors")
    public String doctorList() {
        return "patient/doctor-list";
    }
}
