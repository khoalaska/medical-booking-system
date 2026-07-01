package fpt.medical.controller;

import fpt.medical.dto.RegisterDTO;
import fpt.medical.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

        /*
            Tạo object rỗng để register.html dùng th:object.
        */
        model.addAttribute("registerDTO", new RegisterDTO());

        return "auth/register";
    }

    /*
        Xử lý khi người dùng bấm nút Đăng ký.
    */
    @PostMapping("/register")
    public String processRegister(@ModelAttribute("registerDTO") RegisterDTO registerDTO,
                                  BindingResult bindingResult) {

        /*
            1. Làm sạch họ tên và số điện thoại.
        */
        registerDTO.setFullName(cleanText(registerDTO.getFullName()));
        registerDTO.setPhone(normalizePhone(registerDTO.getPhone()));

        /*
            2. Check họ và tên.
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
            3. Check số điện thoại Việt Nam.

            Sai vì bất cứ lý do gì thì hiện:
            Sai form số Việt Nam
        */
        if (!isValidVietnamPhone(registerDTO.getPhone())) {
            bindingResult.rejectValue(
                    "phone",
                    "phone.invalid",
                    "Sai form số Việt Nam"
            );
        } else if (userService.existsByPhone(registerDTO.getPhone())) {

            /*
                Chỉ khi số điện thoại đúng form mới check trùng DB.
            */
            bindingResult.rejectValue(
                    "phone",
                    "phone.duplicate",
                    "Số điện thoại đã được sử dụng"
            );
        }

        /*
            4. Check mật khẩu.

            Yêu cầu:
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
        } else if (!registerDTO.getPassword().matches("^[0-9]{6,255}$")) {
            bindingResult.rejectValue(
                    "password",
                    "password.invalid",
                    "Mật khẩu chỉ gồm số 0-9, ít nhất 6 ký tự và tối đa 255 ký tự"
            );
        }

        /*
            5. Check nhập lại mật khẩu.
        */
        if (isBlank(registerDTO.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "confirmPassword.blank",
                    "Vui lòng nhập lại mật khẩu"
            );
        } else if (!registerDTO.getConfirmPassword().equals(registerDTO.getPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "confirmPassword.notMatch",
                    "Mật khẩu nhập lại không khớp"
            );
        }

        /*
            6. Nếu có lỗi thì quay lại màn đăng ký.
            Xóa mật khẩu để không giữ lại trên form.
        */
        if (bindingResult.hasErrors()) {
            registerDTO.setPassword("");
            registerDTO.setConfirmPassword("");
            return "auth/register";
        }

        /*
            7. Nếu đúng hết thì lưu DB.
        */
        userService.registerPatient(registerDTO);

        /*
            8. Đăng ký xong quay về login.
        */
        return "redirect:/auth/login";
    }

    /*
        Check số điện thoại Việt Nam.

        Cho phép người dùng nhập:
        0987654321
        0987 654 321
        0987-654-321
        +84987654321
        84987654321

        Sau khi normalize thì phải:
        - Có 10 số
        - Bắt đầu bằng 03, 05, 07, 08, 09
    */
    private boolean isValidVietnamPhone(String phone) {
        if (isBlank(phone)) {
            return false;
        }

        return phone.matches("^0(3|5|7|8|9)[0-9]{8}$");
    }

    /*
        Chuẩn hóa số điện thoại.

        0987 654 321  -> 0987654321
        0987-654-321  -> 0987654321
        +84987654321  -> 0987654321
        84987654321   -> 0987654321
    */
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

    /*
        Xóa khoảng trắng đầu cuối.
    */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }

    /*
        Check rỗng.
    */
    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}