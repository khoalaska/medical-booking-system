package fpt.medical.service;

import fpt.medical.entity.Department;
import org.springframework.data.domain.Page;

public interface DepartmentService {
    Page<Department> getDepartments(String keyword, int page, int size, String sortBy, String sortDir);
    Department getById(Long id);
    Department save(Department department);
    void deleteById(Long id);
    boolean existsByName(String name);
}
