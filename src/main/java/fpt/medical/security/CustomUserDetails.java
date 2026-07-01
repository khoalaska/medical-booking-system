package fpt.medical.security;

import fpt.medical.entity.Role;
import fpt.medical.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    // Constructor: nhận user từ DB truyền vào
    public CustomUserDetails(User user) {
        this.user = user;
    }

    // Lấy quyền của user
    // Ví dụ: ROLE_ADMIN, ROLE_DOCTOR, ROLE_PATIENT
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        List<GrantedAuthority> authorities = new ArrayList<>();

        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
        }

        return authorities;
    }

    // Lấy mật khẩu trong DB
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // Username của mình chính là số điện thoại
    @Override
    public String getUsername() {
        return user.getPhone();
    }

    // Kiểm tra tài khoản có được đăng nhập không
    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    // Sau này cần lấy thông tin user đang login thì dùng hàm này
    public User getUser() {
        return user;
    }
}