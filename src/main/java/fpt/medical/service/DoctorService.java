package fpt.medical.service;

import fpt.medical.dto.DoctorFormDTO;
import fpt.medical.entity.Doctor;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DoctorService {
    //Yen
    List<Doctor> getDoctorsByDepartment(Long departmentId);
    Doctor getById(Long id);

    //Khoa
    Doctor getByUserId(Long userId);

    Page<Doctor> getDoctors(String keyword, Long departmentId, int page, int size,
                             String sortBy, String sortDir);
    Doctor getByIdWithUser(Long id);
    Doctor createDoctor(DoctorFormDTO dto);
    Doctor updateDoctor(DoctorFormDTO dto);
    void deleteById(Long id);
    List<Doctor> getAllForDropdown();
}
