package fpt.medical.controller;

import fpt.medical.entity.TimeSlot;
import fpt.medical.entity.WorkSchedule;
import fpt.medical.exception.ScheduleInUseException;
import fpt.medical.service.TimeSlotAdminService;
import fpt.medical.service.WorkScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/schedules/{scheduleId}/timeslots")
@RequiredArgsConstructor
public class AdminTimeSlotController {

    private final TimeSlotAdminService timeSlotAdminService;
    private final WorkScheduleService workScheduleService;

    @GetMapping
    public String listTimeSlots(@PathVariable("scheduleId") Long scheduleId, Model model) {
        WorkSchedule schedule = workScheduleService.getScheduleById(scheduleId);
        model.addAttribute("schedule", schedule);
        model.addAttribute("timeSlots", timeSlotAdminService.getByScheduleId(scheduleId));
        return "admin/timeslot-list";
    }

    @GetMapping("/new")
    public String showCreateForm(@PathVariable("scheduleId") Long scheduleId, Model model) {
        WorkSchedule schedule = workScheduleService.getScheduleById(scheduleId);
        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("schedule", schedule);
        model.addAttribute("timeSlot", new TimeSlot());
        model.addAttribute("pageTitle", "Thêm khung giờ");
        return "admin/timeslot-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable("scheduleId") Long scheduleId,
            @PathVariable("id") Long id,
            Model model) {
        WorkSchedule schedule = workScheduleService.getScheduleById(scheduleId);
        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("schedule", schedule);
        model.addAttribute("timeSlot", timeSlotAdminService.getById(id));
        model.addAttribute("pageTitle", "Sửa khung giờ");
        return "admin/timeslot-form";
    }

    @PostMapping("/save")
    public String saveTimeSlot(
            @PathVariable("scheduleId") Long scheduleId,
            @Valid @ModelAttribute("timeSlot") TimeSlot timeSlot,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        boolean isEdit = timeSlot.getId() != null;

        if (result.hasErrors()) {
            WorkSchedule schedule = workScheduleService.getScheduleById(scheduleId);
            model.addAttribute("scheduleId", scheduleId);
            model.addAttribute("schedule", schedule);
            model.addAttribute("pageTitle", isEdit ? "Sửa khung giờ" : "Thêm khung giờ");
            return "admin/timeslot-form";
        }

        try {
            if (!isEdit) {
                timeSlotAdminService.create(scheduleId, timeSlot);
            } else {
                timeSlotAdminService.update(timeSlot.getId(), timeSlot);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Lưu khung giờ thành công!");
            return "redirect:/admin/schedules/" + scheduleId + "/timeslots";
        } catch (IllegalArgumentException | ScheduleInUseException e) {
            WorkSchedule schedule = workScheduleService.getScheduleById(scheduleId);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("scheduleId", scheduleId);
            model.addAttribute("schedule", schedule);
            model.addAttribute("pageTitle", isEdit ? "Sửa khung giờ" : "Thêm khung giờ");
            return "admin/timeslot-form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteTimeSlot(
            @PathVariable("scheduleId") Long scheduleId,
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {
        try {
            timeSlotAdminService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa khung giờ thành công!");
        } catch (ScheduleInUseException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi xóa khung giờ.");
        }
        return "redirect:/admin/schedules/" + scheduleId + "/timeslots";
    }
}
