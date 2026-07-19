package fpt.medical.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorFormDTO {

    private Long id;

    private Long userId;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên tối đa 255 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    private String password;

    private Long departmentId;

    @NotBlank(message = "Chuyên khoa không được để trống")
    @Size(max = 255, message = "Chuyên khoa tối đa 255 ký tự")
    private String specialization;

    private String bio;

    @Min(value = 0, message = "Số năm kinh nghiệm không được âm")
    private Integer experienceYears;

    @DecimalMin(value = "0.0", message = "Đánh giá thấp nhất là 0.0")
    @DecimalMax(value = "5.0", message = "Đánh giá cao nhất là 5.0")
    private Double rating;
}
