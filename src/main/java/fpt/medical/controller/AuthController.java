package fpt.medical.controller;

import fpt.medical.dto.RegisterDTO;
import fpt.medical.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /*
        Mở màn hình đăng nhập.
    */
    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login";
    }

    /*
        Mở màn hình đăng ký.
    */
    @GetMapping("/register")
    public String showRegisterPage(Model model) {

        model.addAttribute(
                "registerDTO",
                new RegisterDTO()
        );

        return "auth/register";
    }

    /*
        Xử lý khi người dùng bấm nút Đăng ký.
    */
    @PostMapping("/register")
    public String processRegister(
            @ModelAttribute("registerDTO")
            RegisterDTO registerDTO,
            BindingResult bindingResult
    ) {

        /*
            1. Chuẩn hóa dữ liệu.
        */
        registerDTO.setFullName(
                cleanText(registerDTO.getFullName())
        );

        registerDTO.setPhone(
                normalizePhone(registerDTO.getPhone())
        );

        registerDTO.setEmail(
                normalizeEmail(registerDTO.getEmail())
        );

        /*
            2. Validate họ và tên.
        */
        if (isBlank(registerDTO.getFullName())) {

            bindingResult.rejectValue(
                    "fullName",
                    "fullName.blank",
                    "Vui lòng nhập họ và tên"
            );

        } else if (registerDTO.getFullName().length() > 255) {

            bindingResult.rejectValue(
                    "fullName",
                    "fullName.tooLong",
                    "Họ và tên tối đa 255 ký tự"
            );
        }

        /*
            3. Validate số điện thoại.
        */
        if (!isValidVietnamPhone(registerDTO.getPhone())) {

            bindingResult.rejectValue(
                    "phone",
                    "phone.invalid",
                    "Sai form số Việt Nam"
            );

        } else if (
                userService.existsByPhone(registerDTO.getPhone())
        ) {

            bindingResult.rejectValue(
                    "phone",
                    "phone.duplicate",
                    "Số điện thoại đã được sử dụng"
            );
        }

        /*
            4. Validate email tại Service.
        */
        if (!userService.isValidEmail(registerDTO.getEmail())) {

            bindingResult.rejectValue(
                    "email",
                    "email.invalid",
                    "Email không đúng định dạng"
            );

        } else if (
                userService.existsByEmail(registerDTO.getEmail())
        ) {

            bindingResult.rejectValue(
                    "email",
                    "email.duplicate",
                    "Email đã được sử dụng"
            );
        }

        /*
            5. Validate mật khẩu.

            Mật khẩu:
            - Chỉ gồm số 0-9
            - Ít nhất 6 ký tự
            - Tối đa 255 ký tự
        */
        if (isBlank(registerDTO.getPassword())) {

            bindingResult.rejectValue(
                    "password",
                    "password.blank",
                    "Vui lòng nhập mật khẩu"
            );

        } else if (
                !registerDTO.getPassword()
                        .matches("^[0-9]{6,255}$")
        ) {

            bindingResult.rejectValue(
                    "password",
                    "password.invalid",
                    "Mật khẩu chỉ gồm số 0-9, ít nhất 6 ký tự và tối đa 255 ký tự"
            );
        }

        /*
            6. Validate nhập lại mật khẩu.
        */
        if (isBlank(registerDTO.getConfirmPassword())) {

            bindingResult.rejectValue(
                    "confirmPassword",
                    "confirmPassword.blank",
                    "Vui lòng nhập lại mật khẩu"
            );

        } else if (
                !registerDTO.getConfirmPassword()
                        .equals(registerDTO.getPassword())
        ) {

            bindingResult.rejectValue(
                    "confirmPassword",
                    "confirmPassword.notMatch",
                    "Mật khẩu nhập lại không khớp"
            );
        }

        /*
            7. Nếu có lỗi thì trở lại form đăng ký.
        */
        if (bindingResult.hasErrors()) {

            registerDTO.setPassword("");
            registerDTO.setConfirmPassword("");

            return "auth/register";
        }

        /*
            8. Lưu tài khoản.
        */
        userService.registerPatient(registerDTO);

        return "redirect:/auth/login";
    }

    private boolean isValidVietnamPhone(String phone) {
        if (isBlank(phone)) {
            return false;
        }

        return phone.matches(
                "^0(3|5|7|8|9)[0-9]{8}$"
        );
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }

        String cleanPhone = phone.trim();

        cleanPhone = cleanPhone.replaceAll(
                "[\\s.-]",
                ""
        );

        if (cleanPhone.startsWith("+84")) {

            cleanPhone =
                    "0" + cleanPhone.substring(3);

        } else if (cleanPhone.startsWith("84")) {

            cleanPhone =
                    "0" + cleanPhone.substring(2);
        }

        return cleanPhone;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase();
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}