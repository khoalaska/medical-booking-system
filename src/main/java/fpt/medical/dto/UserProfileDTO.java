package fpt.medical.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileDTO {

    @NotBlank(message = "Vui lòng nhập họ và tên")
    @Size(max = 255, message = "Họ và tên tối đa 255 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
            regexp = "^0(3|5|7|8|9)[0-9]{8}$",
            message = "Số điện thoại không đúng định dạng Việt Nam"
    )
    private String phone;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    private String email;
}