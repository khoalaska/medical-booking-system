package fpt.medical.security;

import fpt.medical.entity.User;
import fpt.medical.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /*
        Hàm này được Spring Security tự gọi khi bấm nút Đăng nhập.

        username ở đây thực ra là số điện thoại.
        Vì lát nữa trong SecurityConfig mình sẽ khai báo:
        .usernameParameter("phone")
    */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByPhone(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy số điện thoại: " + username));

        return new CustomUserDetails(user);
    }
}