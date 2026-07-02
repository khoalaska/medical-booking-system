package fpt.medical.service.impl;

import fpt.medical.entity.Doctor;
import fpt.medical.exception.ResourceNotFoundException;
import fpt.medical.repository.DoctorRepository;
import fpt.medical.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    //Yen
    @Override
    @Transactional(readOnly = true)
    public List<Doctor> getDoctorsByDepartment(Long departmentId) {
        return doctorRepository.findByDepartmentIdWithUser(departmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Doctor getById(Long id) {
        Doctor doctor = doctorRepository.findByIdWithUser(id);
        if (doctor == null) {
            throw new ResourceNotFoundException("Doctor", "id", id);
        }
        return doctor;
    }

    //Khoa
    @Override
    @Transactional(readOnly = true)
    public Doctor getByUserId(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId);
        if (doctor == null) {
            throw new ResourceNotFoundException("Doctor", "userId", userId);
        }
        return doctor;
    }
}
