
package fpt.medical.repository;

import fpt.medical.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Login bằng số điện thoại nên cần tìm user theo phone
    Optional<User> findByPhone(String phone);

    // Sau này làm register sẽ dùng để kiểm tra số điện thoại bị trùng chưa
    boolean existsByPhone(String phone);

    // Kiểm tra trùng email khi tạo User mới (dùng ở module Admin - Doctor)
    boolean existsByEmail(String email);
}
