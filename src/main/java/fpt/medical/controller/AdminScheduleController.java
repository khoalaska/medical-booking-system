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

import fpt.medical.service.DepartmentService;

import java.util.List;

@Controller
@RequestMapping("/admin/schedules")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final WorkScheduleService workScheduleService;
    private final DoctorService doctorService;
    private final DepartmentService departmentService;

    @GetMapping
    public String listSchedules(
            @RequestParam(value = "departmentId", required = false) String departmentIdParam,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "weekStart", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate weekStart,
            Model model) {

        Long departmentId = (departmentIdParam != null && !departmentIdParam.isBlank())
                ? Long.valueOf(departmentIdParam) : null;

        fpt.medical.dto.WeeklyScheduleGridDTO grid =
                workScheduleService.getWeeklyGrid(departmentId, keyword, weekStart);

        model.addAttribute("grid", grid);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("prevWeekStart", grid.getWeekStart().minusWeeks(1));
        model.addAttribute("nextWeekStart", grid.getWeekStart().plusWeeks(1));
        model.addAttribute("doctors", doctorService.getAllForDropdown());
        model.addAttribute("departments", departmentService.getAllForDropdown());

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
            @RequestParam(value = "shiftOption", required = false) String shiftOption,
            Model model,
            RedirectAttributes redirectAttributes) {
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

            boolean isFullDay = "FULL".equals(shiftOption);

            if (workSchedule.getId() == null) {
                if (isFullDay) {
                    workScheduleService.createFullDaySchedule(workSchedule);
                } else {
                    if (shiftOption == null || shiftOption.isBlank()) {
                        throw new IllegalArgumentException("Vui lòng chọn ca làm việc");
                    }
                    workSchedule.setShift(ShiftType.valueOf(shiftOption));
                    workScheduleService.createSchedule(workSchedule);
                }
            } else {
                if (isFullDay) {
                    throw new IllegalArgumentException(
                            "Không thể đổi lịch đang chỉnh sửa thành Cả ngày. Vui lòng tạo lịch cả ngày mới hoặc sửa từng ca riêng.");
                }
                if (shiftOption == null || shiftOption.isBlank()) {
                    throw new IllegalArgumentException("Vui lòng chọn ca làm việc");
                }
                workSchedule.setShift(ShiftType.valueOf(shiftOption));
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

    @PostMapping("/publish")
    public String publishSchedules(
            @RequestParam(value = "doctorId", required = false) String doctorIdParam,
            @RequestParam("fromDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam("toDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            RedirectAttributes redirectAttributes) {
        try {
            Long doctorId = (doctorIdParam != null && !doctorIdParam.isBlank()) ? Long.valueOf(doctorIdParam) : null;
            fpt.medical.dto.PublishScheduleResultDTO result = workScheduleService.publishSchedules(doctorId, fromDate, toDate);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã công bố " + result.getPublishedCount() + " lịch làm việc.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/schedules";
    }

    @GetMapping("/auto-assign")
    public String showAutoAssignForm(Model model) {
        model.addAttribute("departments", departmentService.getAllForDropdown());
        return "admin/schedule-auto-assign";
    }

    @PostMapping("/auto-assign")
    public String autoAssign(
            @RequestParam("departmentId") Long departmentId,
            @RequestParam("fromDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam("toDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            RedirectAttributes redirectAttributes) {
        try {
            fpt.medical.dto.AutoAssignResultDTO result = workScheduleService.autoAssignSchedules(departmentId, fromDate, toDate);
            String msg = "Đã tạo " + result.getCreatedCount() + " lịch làm việc (bản nháp).";
            if (!result.getUnfilledWarnings().isEmpty()) {
                msg += " Cảnh báo: " + String.join("; ", result.getUnfilledWarnings());
            }
            redirectAttributes.addFlashAttribute("successMessage", msg);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/schedules";
    }

    @PostMapping(value = "/move", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public fpt.medical.dto.MoveScheduleResultDTO moveSchedule(
            @RequestBody fpt.medical.dto.MoveScheduleRequestDTO request) {
        return workScheduleService.moveOrSwapSchedules(request);
    }
}
