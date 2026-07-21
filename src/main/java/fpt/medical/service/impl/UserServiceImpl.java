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
import fpt.medical.dto.UserProfileDTO;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    /*
        Email phải có dạng:

        example@gmail.com
        user.name@fpt.edu.vn

        Không chấp nhận:

        abc
        abc@
        @gmail.com
        abc@gmail
    */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@" +
                    "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?" +
                    "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

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
    public boolean existsByEmail(String email) {
        String cleanEmail = normalizeEmail(email);

        return userRepository.existsByEmail(cleanEmail);
    }

    /*
        Validate email tại Service theo yêu cầu.

        Email:
        - Không được rỗng
        - Không quá 255 ký tự
        - Phải đúng định dạng
    */
    @Override
    public boolean isValidEmail(String email) {
        String cleanEmail = normalizeEmail(email);

        return !cleanEmail.isEmpty()
                && cleanEmail.length() <= 255
                && EMAIL_PATTERN.matcher(cleanEmail).matches();
    }

    @Override
    public void registerPatient(RegisterDTO registerDTO) {

        String cleanPhone = normalizePhone(registerDTO.getPhone());
        String cleanEmail = normalizeEmail(registerDTO.getEmail());

        /*
            Kiểm tra lại tại Service trước khi lưu để tránh việc
            gọi registerPatient từ nơi khác mà không validate email.
        */
        if (!isValidEmail(cleanEmail)) {
            throw new IllegalArgumentException(
                    "Email không đúng định dạng"
            );
        }

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException(
                    "Email đã được sử dụng"
            );
        }

        Role patientRole = roleRepository.findByName("ROLE_PATIENT")
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy ROLE_PATIENT"
                        )
                );

        User user = new User();

        user.setFullName(registerDTO.getFullName());
        user.setPhone(cleanPhone);
        user.setEmail(cleanEmail);
        user.setPassword(
                passwordEncoder.encode(registerDTO.getPassword())
        );
        user.setEnabled(true);

        user.getRoles().add(patientRole);

        User savedUser = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(savedUser);

        patientRepository.save(patient);
    }

    /*
        Chuẩn hóa email:

        "  TEST@GMAIL.COM  "
        thành:
        "test@gmail.com"
    */
    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase();
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

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tài khoản"
                        )
                );

        UserProfileDTO profileDTO = new UserProfileDTO();

        profileDTO.setFullName(user.getFullName());
        profileDTO.setPhone(user.getPhone());
        profileDTO.setEmail(user.getEmail());

        return profileDTO;
    }

    @Override
    public void updateProfile(
            Long userId,
            UserProfileDTO userProfileDTO
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tài khoản"
                        )
                );

        String cleanPhone =
                normalizePhone(userProfileDTO.getPhone());

        String cleanEmail =
                normalizeEmail(userProfileDTO.getEmail());

        String cleanFullName =
                userProfileDTO.getFullName().trim();

        /*
         * Chỉ kiểm tra trùng khi số điện thoại thật sự thay đổi.
         * Tận dụng existsByPhone() đang có.
         */
        if (!cleanPhone.equals(user.getPhone())
                && existsByPhone(cleanPhone)) {

            throw new IllegalArgumentException(
                    "Số điện thoại đã được sử dụng"
            );
        }

        /*
         * Tận dụng isValidEmail() đang có trong service.
         */
        if (!isValidEmail(cleanEmail)) {
            throw new IllegalArgumentException(
                    "Email không đúng định dạng"
            );
        }

        /*
         * Chỉ kiểm tra trùng khi email thật sự thay đổi.
         * Tận dụng existsByEmail() đang có.
         */
        if (!cleanEmail.equalsIgnoreCase(user.getEmail())
                && existsByEmail(cleanEmail)) {

            throw new IllegalArgumentException(
                    "Email đã được sử dụng"
            );
        }

        user.setFullName(cleanFullName);
        user.setPhone(cleanPhone);
        user.setEmail(cleanEmail);

        userRepository.save(user);
    }
}