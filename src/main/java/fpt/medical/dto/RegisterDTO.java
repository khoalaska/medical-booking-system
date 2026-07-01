package fpt.medical.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {

    //@NotBlank
    private String phone;

    //@NotBlank
    private String fullName;

   // @NotBlank
   // @Email
    //private String email;

   // @NotBlank
    //@Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    //@NotBlank
    private String confirmPassword;

}
