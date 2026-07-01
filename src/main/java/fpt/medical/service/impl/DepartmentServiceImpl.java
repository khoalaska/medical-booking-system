package fpt.medical.service.impl;

import fpt.medical.entity.Department;
import fpt.medical.exception.DepartmentInUseException;
import fpt.medical.exception.DuplicateRecordException;
import fpt.medical.exception.ResourceNotFoundException;
import fpt.medical.repository.DepartmentRepository;
import fpt.medical.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private static final java.util.Set<String> ALLOWED_SORT_FIELDS =
        java.util.Set.of("id", "name");

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Department> getDepartments(String keyword, int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 15) {
            size = 15;
        }
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "name";
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "name";
        }

        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        if (keyword == null || keyword.trim().isEmpty()) {
            return departmentRepository.findAll(pageable);
        } else {
            return departmentRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Department getById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
    }

    @Override
    public Department save(Department department) {
        if (department.getId() == null) {
            if (departmentRepository.existsByNameIgnoreCase(department.getName())) {
                throw new DuplicateRecordException("Department", "name", department.getName());
            }
        } else {
            if (departmentRepository.existsByNameIgnoreCaseAndIdNot(department.getName(), department.getId())) {
                throw new DuplicateRecordException("Department", "name", department.getName());
            }
        }
        return departmentRepository.save(department);
    }

    @Override
    public void deleteById(Long id) {
        Department department = getById(id);
        if (department.getDoctors() != null && !department.getDoctors().isEmpty()) {
            throw new DepartmentInUseException("Không thể xóa phòng ban vì vẫn còn bác sĩ thuộc phòng ban này.");
        }
        departmentRepository.delete(department);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return departmentRepository.existsByNameIgnoreCase(name);
    }
}
