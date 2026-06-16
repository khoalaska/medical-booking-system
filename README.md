# 🏥 Medical Appointment Booking System

Nền tảng đặt lịch khám trực tuyến cho phòng khám / bệnh viện nhỏ.

## 🛠️ Tech Stack
- Java 21 + Spring Boot 3.5
- Spring MVC, Spring Security, Spring Data JPA
- Thymeleaf, SQL Server, Lombok

---

## ⚙️ Cách Chạy Project

### 1. Yêu cầu
- JDK 21+
- SQL Server (bất kỳ phiên bản nào)
- IntelliJ IDEA (khuyến nghị)

### 2. Tạo Database
Mở SQL Server Management Studio và chạy:
```sql
CREATE DATABASE medical_booking_db;
```

### 3. Cấu hình kết nối DB
Mở file `src/main/resources/application.properties` và chỉnh:
```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=medical_booking_db;encrypt=true;trustServerCertificate=true
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

> Nếu dùng SQL Server Express:
> ```
> jdbc:sqlserver://localhost\SQLEXPRESS:1433;databaseName=medical_booking_db;...
> ```

### 4. Chạy ứng dụng
```bash
# Dùng Maven Wrapper (không cần cài Maven)
./mvnw spring-boot:run

# Hoặc trong IntelliJ: Run > MedicalBookingSystemApplication
```

### 5. Truy cập
```
http://localhost:8080
```

---

## 👥 Roles
| Role | Mô tả |
|---|---|
| `ROLE_ADMIN` | Quản lý bác sĩ, khoa, lịch làm việc |
| `ROLE_DOCTOR` | Xem lịch, ghi chẩn đoán, kê đơn |
| `ROLE_PATIENT` | Đặt lịch, xem hồ sơ bệnh án |

---

## 📁 Cấu Trúc Project
```
src/main/java/fpt/medical/
├── config/         # Cấu hình Spring
├── controller/     # Xử lý HTTP request
├── dto/            # Data Transfer Objects
├── entity/         # JPA Entities
├── enums/          # Enum types
├── exception/      # Xử lý ngoại lệ
├── repository/     # Spring Data JPA
├── security/       # Spring Security
├── service/        # Business logic
│   └── impl/
├── util/           # Tiện ích
└── validator/      # Custom validators
```
