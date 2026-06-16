package fpt.medical.dto;

import lombok.Data;

@Data
public class DoctorDTO {

    private Long id;
    private String fullName;
    private String specialty;
    private String phone;
    private String email;
    private Long departmentId;
    private String avatarUrl;
}
