package fpt.medical.repository;

import fpt.medical.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Page<Department> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    List<Department> findAllByOrderByNameAsc();

    @Query("select d from Department d where d.doctors is empty")
    List<Department> findDepartmentsWithoutDoctors();

    @Query("select new fpt.medical.dto.DoctorCountByDepartmentDTO(d.name, count(doc)) from Department d left join d.doctors doc group by d.id, d.name order by d.name asc")
    List<fpt.medical.dto.DoctorCountByDepartmentDTO> countDoctorsPerDepartment();
}
