package fpt.medical.service;

import fpt.medical.entity.Doctor;

import java.util.List;

public interface DoctorService {
    //Yen
    List<Doctor> getDoctorsByDepartment(Long departmentId);
    Doctor getById(Long id);
}
