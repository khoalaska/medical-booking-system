package fpt.medical.service;

import fpt.medical.dto.RegisterDTO;
import fpt.medical.dto.UserProfileDTO;

public interface UserService {

    boolean existsByPhone(String phone);

    void registerPatient(RegisterDTO registerDTO);
    boolean existsByEmail(String email);

    boolean isValidEmail(String email);
    UserProfileDTO getProfile(Long userId);

    void updateProfile(Long userId, UserProfileDTO userProfileDTO);
}