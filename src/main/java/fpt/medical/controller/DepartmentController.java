package fpt.medical.controller;

import fpt.medical.entity.Department;
import fpt.medical.exception.DuplicateRecordException;
import fpt.medical.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public String listDepartments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestParam(value = "sortBy", defaultValue = "name") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir,
            Model model) {

        Page<Department> departmentPage = departmentService.getDepartments(keyword, page, size, sortBy, sortDir);

        model.addAttribute("departments", departmentPage.getContent());
        model.addAttribute("departmentPage", departmentPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", departmentPage.getTotalPages());
        model.addAttribute("totalItems", departmentPage.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equalsIgnoreCase("asc") ? "desc" : "asc");

        return "admin/department-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("department", new Department());
        model.addAttribute("pageTitle", "Thêm mới phòng ban");
        return "admin/department-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Department department = departmentService.getById(id);
        model.addAttribute("department", department);
        model.addAttribute("pageTitle", "Chỉnh sửa phòng ban");
        return "admin/department-form";
    }

    @PostMapping("/save")
    public String saveDepartment(@Valid @ModelAttribute("department") Department department,
                                 BindingResult result,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", department.getId() == null ? "Thêm mới phòng ban" : "Chỉnh sửa phòng ban");
            return "admin/department-form";
        }
        try {
            departmentService.save(department);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu phòng ban thành công!");
            return "redirect:/departments";
        } catch (DuplicateRecordException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", department.getId() == null ? "Thêm mới phòng ban" : "Chỉnh sửa phòng ban");
            return "admin/department-form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteDepartment(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            departmentService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa phòng ban thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/departments";
    }
}
