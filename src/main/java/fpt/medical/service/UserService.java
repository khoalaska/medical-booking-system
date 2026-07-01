package fpt.medical.service;

import fpt.medical.dto.RegisterDTO;

public interface UserService {

    boolean existsByPhone(String phone);

    void registerPatient(RegisterDTO registerDTO);
}