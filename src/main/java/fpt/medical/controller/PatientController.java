package fpt.medical.controller;

import fpt.medical.dto.UserProfileDTO;
import fpt.medical.entity.Department;
import fpt.medical.entity.Doctor;
import fpt.medical.entity.TimeSlot;
import fpt.medical.enums.ShiftType;
import fpt.medical.exception.ResourceNotFoundException;
import fpt.medical.security.CustomUserDetails;
import fpt.medical.service.AppointmentService;
import fpt.medical.service.DepartmentService;
import fpt.medical.service.DoctorService;
import fpt.medical.service.UserService;
import fpt.medical.service.WorkScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final DepartmentService departmentService;
    private final DoctorService doctorService;
    private final WorkScheduleService workScheduleService;
    private final UserService userService;
    private final AppointmentService appointmentService;

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

    @PostMapping("/book-appointment")
    public String confirmBooking(
            @RequestParam Long timeSlotId,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            appointmentService.bookAppointment(
                    currentUser.getUser().getId(), timeSlotId, notes);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đặt lịch khám thành công.");
        } catch (IllegalArgumentException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/patients/book-appointment";
    }

    @GetMapping("/doctors")
    public String doctorList(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        List<Department> departments = departmentService.getAllForDropdown();
        Page<Doctor> doctorPage = doctorService.getDoctors(keyword, departmentId, 0, 50, "id", "asc");

        model.addAttribute("departments", departments);
        model.addAttribute("doctors", doctorPage.getContent());
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("keyword", keyword);

        return "patient/doctor-list";
    }

    @GetMapping("/profile")
    public String showProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {

        model.addAttribute(
                "userProfileDTO",
                userService.getProfile(
                        userDetails.getUser().getId()
                )
        );

        return "patient/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Valid
            @ModelAttribute("userProfileDTO")
            UserProfileDTO userProfileDTO,

            BindingResult bindingResult,

            RedirectAttributes redirectAttributes
    ) {

        if (bindingResult.hasErrors()) {
            return "patient/profile";
        }

        try {

            userService.updateProfile(
                    userDetails.getUser().getId(),
                    userProfileDTO
            );

        } catch (IllegalArgumentException exception) {

            String message = exception.getMessage();

            if ("Số điện thoại đã được sử dụng"
                    .equals(message)) {

                bindingResult.rejectValue(
                        "phone",
                        "phone.duplicate",
                        message
                );

            } else if (
                    "Email đã được sử dụng"
                            .equals(message)
                            || "Email không đúng định dạng"
                            .equals(message)
            ) {

                bindingResult.rejectValue(
                        "email",
                        "email.invalid",
                        message
                );

            } else {

                bindingResult.reject(
                        "profile.error",
                        message
                );
            }

            return "patient/profile";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Cập nhật hồ sơ thành công"
        );

        return "redirect:/patients/profile";
    }
}
