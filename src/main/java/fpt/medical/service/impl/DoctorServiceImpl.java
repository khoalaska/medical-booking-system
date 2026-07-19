package fpt.medical.service.impl;

import fpt.medical.dto.DoctorFormDTO;
import fpt.medical.entity.Department;
import fpt.medical.entity.Doctor;
import fpt.medical.entity.Role;
import fpt.medical.entity.User;
import fpt.medical.exception.DoctorInUseException;
import fpt.medical.exception.DuplicateRecordException;
import fpt.medical.exception.ResourceNotFoundException;
import fpt.medical.repository.DepartmentRepository;
import fpt.medical.repository.DoctorRepository;
import fpt.medical.repository.RoleRepository;
import fpt.medical.repository.UserRepository;
import fpt.medical.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private static final java.util.Set<String> ALLOWED_SORT_FIELDS =
            java.util.Set.of("id", "specialization", "rating");

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Override
    @Transactional(readOnly = true)
    public Page<Doctor> getDoctors(String keyword, Long departmentId, int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 15) {
            size = 15;
        }
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "id";
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "id";
        }

        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasDepartment = departmentId != null;

        if (hasDepartment && hasKeyword) {
            return doctorRepository.findByDepartmentIdAndUser_FullNameContainingIgnoreCase(departmentId, keyword.trim(), pageable);
        } else if (hasKeyword) {
            return doctorRepository.findByUser_FullNameContainingIgnoreCase(keyword.trim(), pageable);
        } else if (hasDepartment) {
            return doctorRepository.findByDepartmentId(departmentId, pageable);
        } else {
            return doctorRepository.findAll(pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Doctor getByIdWithUser(Long id) {
        Doctor doctor = doctorRepository.findByIdWithUser(id);
        if (doctor == null) {
            throw new ResourceNotFoundException("Doctor", "id", id);
        }
        return doctor;
    }

    @Override
    public Doctor createDoctor(DoctorFormDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu là bắt buộc khi tạo bác sĩ mới");
        }
        if (userRepository.existsByPhone(dto.getPhone())) {
            throw new DuplicateRecordException("User", "phone", dto.getPhone());
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateRecordException("User", "email", dto.getEmail());
        }

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .password(passwordEncoder.encode(dto.getPassword()))
                .enabled(true)
                .build();

        Role doctorRole = roleRepository.findByName("ROLE_DOCTOR")
                .orElseThrow(() -> new IllegalStateException("Role ROLE_DOCTOR chưa được khởi tạo trong hệ thống"));

        user.setRoles(Set.of(doctorRole));
        User savedUser = userRepository.save(user);

        Department department = dto.getDepartmentId() != null
                ? departmentRepository.findById(dto.getDepartmentId()).orElse(null)
                : null;

        Doctor doctor = Doctor.builder()
                .user(savedUser)
                .department(department)
                .specialization(dto.getSpecialization())
                .bio(dto.getBio())
                .experienceYears(dto.getExperienceYears())
                .rating(0.0)
                .build();

        return doctorRepository.save(doctor);
    }

    @Override
    public Doctor updateDoctor(DoctorFormDTO dto) {
        Doctor doctor = getByIdWithUser(dto.getId());
        User user = doctor.getUser();

        if (!user.getEmail().equalsIgnoreCase(dto.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new DuplicateRecordException("User", "email", dto.getEmail());
            }
        }
        if (!user.getPhone().equals(dto.getPhone())) {
            if (userRepository.existsByPhone(dto.getPhone())) {
                throw new DuplicateRecordException("User", "phone", dto.getPhone());
            }
        }

        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());

        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        doctor.setSpecialization(dto.getSpecialization());
        doctor.setBio(dto.getBio());
        doctor.setExperienceYears(dto.getExperienceYears());

        Department department = dto.getDepartmentId() != null
                ? departmentRepository.findById(dto.getDepartmentId()).orElse(null)
                : null;
        doctor.setDepartment(department);

        userRepository.save(user);
        return doctorRepository.save(doctor);
    }

    @Override
    public void deleteById(Long id) {
        Doctor doctor = getByIdWithUser(id);
        if (doctor.getAppointments() != null && !doctor.getAppointments().isEmpty()) {
            throw new DoctorInUseException("Không thể xóa bác sĩ vì đang có lịch hẹn liên quan.");
        }
        doctorRepository.delete(doctor);
        userRepository.delete(doctor.getUser());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> getAllForDropdown() {
        return doctorRepository.findAllWithUser();
    }
}
