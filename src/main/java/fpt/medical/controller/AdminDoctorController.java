package fpt.medical.controller;

import fpt.medical.dto.DoctorFormDTO;
import fpt.medical.entity.Department;
import fpt.medical.entity.Doctor;
import fpt.medical.exception.DoctorInUseException;
import fpt.medical.exception.DuplicateRecordException;
import fpt.medical.service.DepartmentService;
import fpt.medical.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/doctors")
@RequiredArgsConstructor
public class AdminDoctorController {

    private final DoctorService doctorService;
    private final DepartmentService departmentService;

    private List<Department> getActiveDepartments() {
        return departmentService.getAllForDropdown();
    }

    @GetMapping
    public String listDoctors(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "departmentId", required = false) String departmentIdParam,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir,
            Model model) {
        Long departmentId = (departmentIdParam != null && !departmentIdParam.isBlank())
                ? Long.valueOf(departmentIdParam) : null;
        Page<Doctor> doctorPage = doctorService.getDoctors(keyword, departmentId, page, size, sortBy, sortDir);

        model.addAttribute("doctors", doctorPage.getContent());
        model.addAttribute("doctorPage", doctorPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", doctorPage.getTotalPages());
        model.addAttribute("totalItems", doctorPage.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equalsIgnoreCase("asc") ? "desc" : "asc");
        model.addAttribute("departments", getActiveDepartments());

        return "admin/doctor-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("doctorForm", new DoctorFormDTO());
        model.addAttribute("departments", getActiveDepartments());
        model.addAttribute("pageTitle", "Thêm mới bác sĩ");
        model.addAttribute("isEdit", false);
        return "admin/doctor-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Doctor doctor = doctorService.getByIdWithUser(id);
        DoctorFormDTO dto = mapToDTO(doctor);

        model.addAttribute("doctorForm", dto);
        model.addAttribute("departments", getActiveDepartments());
        model.addAttribute("pageTitle", "Chỉnh sửa bác sĩ");
        model.addAttribute("isEdit", true);
        return "admin/doctor-form";
    }

    @PostMapping("/save")
    public String saveDoctor(
            @Valid @ModelAttribute("doctorForm") DoctorFormDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        boolean isEdit = dto.getId() != null;

        if (result.hasErrors()) {
            model.addAttribute("departments", getActiveDepartments());
            model.addAttribute("pageTitle", isEdit ? "Chỉnh sửa bác sĩ" : "Thêm mới bác sĩ");
            model.addAttribute("isEdit", isEdit);
            return "admin/doctor-form";
        }

        try {
            if (!isEdit) {
                doctorService.createDoctor(dto);
            } else {
                doctorService.updateDoctor(dto);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Lưu bác sĩ thành công!");
            return "redirect:/admin/doctors";
        } catch (DuplicateRecordException | IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("departments", getActiveDepartments());
            model.addAttribute("pageTitle", isEdit ? "Chỉnh sửa bác sĩ" : "Thêm mới bác sĩ");
            model.addAttribute("isEdit", isEdit);
            return "admin/doctor-form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteDoctor(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            doctorService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa bác sĩ thành công!");
        } catch (DoctorInUseException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi xóa bác sĩ.");
        }
        return "redirect:/admin/doctors";
    }

    private DoctorFormDTO mapToDTO(Doctor doctor) {
        return DoctorFormDTO.builder()
                .id(doctor.getId())
                .userId(doctor.getUser().getId())
                .fullName(doctor.getUser().getFullName())
                .email(doctor.getUser().getEmail())
                .phone(doctor.getUser().getPhone())
                .departmentId(doctor.getDepartment() != null ? doctor.getDepartment().getId() : null)
                .specialization(doctor.getSpecialization())
                .bio(doctor.getBio())
                .experienceYears(doctor.getExperienceYears())
                .rating(doctor.getRating())
                .build();
    }
}
