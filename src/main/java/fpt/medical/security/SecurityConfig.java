package fpt.medical.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                /*
                    Nói cho Spring Security biết:
                    Khi login thì đi tìm user bằng CustomUserDetailsService của mình.
                */
                .userDetailsService(customUserDetailsService)

                /*
                    Phân quyền đường dẫn.
                */
                .authorizeHttpRequests(auth -> auth

                        /*
                            Những đường dẫn này ai cũng vào được.
                            Nếu không permit /auth/login thì chưa login sẽ không mở được login.
                        */
                        .requestMatchers(
                                "/",
                                "/error",
                                "/auth/login",
                                "/auth/register",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        /*
                            Admin mới vào được trang admin.
                        */
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        /*
                            Bác sĩ mới vào được trang doctor.
                        */
                        .requestMatchers("/doctor/**").hasRole("DOCTOR")

                        /*
                            Bệnh nhân mới vào được trang patient.
                            Mình để cả /patient/** và /patients/** để tránh lệch URL với nhóm.
                        */
                        .requestMatchers("/patient/**", "/patients/**").hasRole("PATIENT")

                        /*
                            Những trang còn lại phải đăng nhập.
                        */
                        .anyRequest().authenticated()
                )

                /*
                    Cấu hình đăng nhập.
                */
                .formLogin(form -> form

                        /*
                            Đây là trang login mình tự code.
                            Không dùng màn mặc định "Please sign in" nữa.
                        */
                        .loginPage("/auth/login")

                        /*
                            Khi bấm nút Đăng nhập trong form,
                            form sẽ POST tới /login.
                        */
                        .loginProcessingUrl("/login")

                        /*
                            Input đăng nhập trong login.html có name="phone".
                            Spring Security sẽ lấy giá trị này làm username.
                        */
                        .usernameParameter("phone")

                        /*
                            Input mật khẩu trong login.html có name="password".
                        */
                        .passwordParameter("password")

                        /*
                            Login đúng thì chuyển trang theo role.
                        */
                        .successHandler(authenticationSuccessHandler)

                        /*
                            Login sai thì quay lại login và hiện thông báo lỗi.
                        */
                        .failureUrl("/auth/login?error")

                        .permitAll()
                )

                /*
                    Cấu hình đăng xuất.
                */
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/auth/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    /*
        Password trong DB là BCrypt.
        Nên mình dùng BCryptPasswordEncoder để Spring so sánh mật khẩu.
    */
    //@Bean
    //public PasswordEncoder passwordEncoder() {
      //  return new BCryptPasswordEncoder();
    //}
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {

            @Override
            public String encode(CharSequence rawPassword) {
                return rawPassword.toString();
            }

            @Override
            public boolean matches(CharSequence rawPassword, String storedPassword) {
                return rawPassword.toString().equals(storedPassword);
            }
        };
    }
}