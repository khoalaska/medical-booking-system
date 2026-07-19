package fpt.medical.controller;

import fpt.medical.entity.Doctor;
import fpt.medical.entity.WorkSchedule;
import fpt.medical.enums.ShiftType;
import fpt.medical.exception.DuplicateRecordException;
import fpt.medical.exception.ScheduleInUseException;
import fpt.medical.service.DoctorService;
import fpt.medical.service.WorkScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/schedules")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final WorkScheduleService workScheduleService;
    private final DoctorService doctorService;

    @GetMapping
    public String listSchedules(
            @RequestParam(value = "doctorId", required = false) String doctorIdParam,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestParam(value = "sortBy", defaultValue = "workDate") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
            Model model) {

        Long doctorId = (doctorIdParam != null && !doctorIdParam.isBlank())
                ? Long.valueOf(doctorIdParam) : null;

        Page<WorkSchedule> schedulePage = workScheduleService.getSchedules(doctorId, page, size, sortBy, sortDir);

        model.addAttribute("schedules", schedulePage.getContent());
        model.addAttribute("schedulePage", schedulePage);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", schedulePage.getTotalPages());
        model.addAttribute("totalItems", schedulePage.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equalsIgnoreCase("asc") ? "desc" : "asc");
        model.addAttribute("doctors", doctorService.getAllForDropdown());

        return "admin/schedule-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("doctors", doctorService.getAllForDropdown());
        model.addAttribute("workSchedule", new WorkSchedule());
        model.addAttribute("pageTitle", "Thêm mới lịch làm việc");
        model.addAttribute("shiftTypes", ShiftType.values());
        return "admin/schedule-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        WorkSchedule schedule = workScheduleService.getScheduleById(id);
        model.addAttribute("workSchedule", schedule);
        model.addAttribute("doctors", doctorService.getAllForDropdown());
        model.addAttribute("pageTitle", "Chỉnh sửa lịch làm việc");
        model.addAttribute("shiftTypes", ShiftType.values());
        return "admin/schedule-form";
    }

    @PostMapping("/save")
    public String saveSchedule(
            @ModelAttribute WorkSchedule workSchedule,
            BindingResult result,
            @RequestParam(value = "doctorId", required = false) String doctorIdParam,
            Model model,
            RedirectAttributes redirectAttributes) {

        // TODO: cân nhắc tạo DTO riêng nếu cần validate chi tiết hơn
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Dữ liệu nhập không hợp lệ, vui lòng kiểm tra lại.");
            model.addAttribute("doctors", doctorService.getAllForDropdown());
            model.addAttribute("pageTitle", workSchedule.getId() == null ? "Thêm mới lịch làm việc" : "Chỉnh sửa lịch làm việc");
            model.addAttribute("shiftTypes", ShiftType.values());
            return "admin/schedule-form";
        }
        try {
            if (doctorIdParam == null || doctorIdParam.isBlank()) {
                throw new IllegalArgumentException("Vui lòng chọn bác sĩ cho lịch làm việc");
            }
            Long doctorId = Long.valueOf(doctorIdParam);
            Doctor doctor = doctorService.getByIdWithUser(doctorId);
            workSchedule.setDoctor(doctor);

            if (workSchedule.getId() == null) {
                workScheduleService.createSchedule(workSchedule);
            } else {
                workScheduleService.updateSchedule(workSchedule.getId(), workSchedule);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Lưu lịch làm việc thành công!");
            return "redirect:/admin/schedules";
        } catch (DuplicateRecordException | IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("doctors", doctorService.getAllForDropdown());
            model.addAttribute("pageTitle", workSchedule.getId() == null ? "Thêm mới lịch làm việc" : "Chỉnh sửa lịch làm việc");
            model.addAttribute("shiftTypes", ShiftType.values());
            return "admin/schedule-form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteSchedule(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            workScheduleService.deleteSchedule(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa lịch làm việc thành công!");
        } catch (ScheduleInUseException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi xóa lịch làm việc.");
        }
        return "redirect:/admin/schedules";
    }
}
