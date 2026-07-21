package fpt.medical.dto;

import lombok.Data;

@Data
public class RegisterDTO {

    private String phone;

    private String fullName;

    private String email;

    private String password;

    private String confirmPassword;
}