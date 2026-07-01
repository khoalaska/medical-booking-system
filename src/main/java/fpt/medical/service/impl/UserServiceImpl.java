package fpt.medical.service.impl;

import fpt.medical.dto.RegisterDTO;
import fpt.medical.entity.Patient;
import fpt.medical.entity.Role;
import fpt.medical.entity.User;
import fpt.medical.repository.PatientRepository;
import fpt.medical.repository.RoleRepository;
import fpt.medical.repository.UserRepository;
import fpt.medical.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean existsByPhone(String phone) {
        String cleanPhone = normalizePhone(phone);
        return userRepository.existsByPhone(cleanPhone);
    }

    @Override
    public void registerPatient(RegisterDTO registerDTO) {

        String cleanPhone = normalizePhone(registerDTO.getPhone());

        Role patientRole = roleRepository.findByName("ROLE_PATIENT")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ROLE_PATIENT"));

        User user = new User();
        user.setFullName(registerDTO.getFullName());
        user.setPhone(cleanPhone);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEnabled(true);

        /*
            Nếu DB email đã cho NULL thì không cần setEmail.
            Nếu DB vẫn NOT NULL thì mở dòng dưới.
        */
        // user.setEmail(cleanPhone + "@medical.local");

        user.getRoles().add(patientRole);

        User savedUser = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(savedUser);

        patientRepository.save(patient);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }

        String cleanPhone = phone.trim();

        cleanPhone = cleanPhone.replaceAll("[\\s.-]", "");

        if (cleanPhone.startsWith("+84")) {
            cleanPhone = "0" + cleanPhone.substring(3);
        } else if (cleanPhone.startsWith("84")) {
            cleanPhone = "0" + cleanPhone.substring(2);
        }

        return cleanPhone;
    }
}