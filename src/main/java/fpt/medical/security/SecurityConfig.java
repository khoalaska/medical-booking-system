package fpt.medical.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // TODO: Bạn (người phụ trách Security) sẽ implement đầy đủ phần này sau
    // Hiện tại: cho phép TẤT CẢ request đi qua, không cần đăng nhập
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()   // ← mở toàn bộ, không chặn gì cả
                )
                .csrf(AbstractHttpConfigurer::disable); // ← tắt CSRF để POST form hoạt động

        return http.build();
    }
}