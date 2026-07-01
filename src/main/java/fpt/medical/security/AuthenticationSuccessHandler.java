package fpt.medical.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    /*
        Hàm này tự chạy sau khi đăng nhập thành công.

        Mình kiểm tra role của tài khoản:
        - ROLE_ADMIN   -> /admin/dashboard
        - ROLE_DOCTOR  -> /doctor/dashboard
        - ROLE_PATIENT -> /
    */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));

        boolean isDoctor = authentication.getAuthorities()
                .stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_DOCTOR"));

        boolean isPatient = authentication.getAuthorities()
                .stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_PATIENT"));

        String contextPath = request.getContextPath();

        if (isAdmin) {
            response.sendRedirect(contextPath + "/admin/dashboard");
        } else if (isDoctor) {
            response.sendRedirect(contextPath + "/doctor/dashboard");
        } else if (isPatient) {
            response.sendRedirect(contextPath + "/patients/book-appointment");
        } else {
            response.sendRedirect(contextPath + "/auth/login?error");
        }
    }
}