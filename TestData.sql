-- ============================================================
--  TEST DATA cho MODULE BÁC SĨ (Khoa)
--  Chạy SAU khi đã tạo DB bằng ChangeDB.sql.
--
--  Bác sĩ dùng để test: BS. Nguyễn Văn An
--    - Đăng nhập: số điện thoại 0901000002 / mật khẩu 123
--
--  Script này tạo:
--    - Đặt mật khẩu plaintext '123' cho MỌI tài khoản (để đăng nhập test được)
--    - 20 bệnh nhân test (để test phân trang danh sách bệnh nhân > 15)
--    - Ca làm EVENING (18h-21h) cho bác sĩ test trong 6 ngày (hôm kia .. 3 ngày tới)
--    - Lịch hẹn HÔM NAY đủ 4 trạng thái (chờ / đã xác nhận / đã khám / đã hủy)
--    - Lịch sử khám (hồ sơ bệnh án + đơn thuốc) cho các buổi đã khám
--
--  Có thể chạy lại nhiều lần (dùng NOT EXISTS để tránh trùng).
-- ============================================================

USE MedicalAppointmentSystem;
GO

-- ============================================================
--  0. (TÙY CHỌN) Đặt mật khẩu plaintext để đăng nhập test
--     Code hiện so sánh mật khẩu dạng plaintext, nên set '123' cho mọi user.
--     Nếu login của bạn đã chạy được thì có thể bỏ qua đoạn này.
-- ============================================================
UPDATE dbo.users SET password = '123';
GO

-- ============================================================
--  1. TẠO 20 BỆNH NHÂN TEST
-- ============================================================

-- 1a. Tạo tài khoản người dùng cho 20 bệnh nhân
;WITH nums AS (
    SELECT TOP (20) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM sys.all_objects
)
INSERT INTO dbo.users (full_name, email, password, phone, enabled)
SELECT
    CONCAT(N'Bệnh nhân Test ', FORMAT(n, '00')),
    CONCAT('patient.test', n, '@gmail.com'),
    '123',
    CONCAT('0912', RIGHT('000000' + CAST(n AS VARCHAR(6)), 6)),
    1
FROM nums
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.users u
    WHERE u.email = CONCAT('patient.test', nums.n, '@gmail.com')
);
GO

-- 1b. Gán quyền ROLE_PATIENT cho các tài khoản test
INSERT INTO dbo.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM dbo.users u
CROSS JOIN dbo.roles r
WHERE u.email LIKE 'patient.test%@gmail.com'
  AND r.name = 'ROLE_PATIENT'
  AND NOT EXISTS (
      SELECT 1 FROM dbo.user_roles ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
GO

-- 1c. Tạo hồ sơ bệnh nhân (patients) cho các tài khoản test
INSERT INTO dbo.patients (user_id, date_of_birth, gender, address, blood_type)
SELECT
    u.id,
    DATEADD(YEAR, -(18 + (CAST(RIGHT(u.phone, 2) AS INT) % 45)), CAST(GETDATE() AS DATE)),
    CASE WHEN CAST(RIGHT(u.phone, 2) AS INT) % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
    N'Địa chỉ test, TP.HCM',
    CASE CAST(RIGHT(u.phone, 1) AS INT) % 4
        WHEN 0 THEN 'O+'
        WHEN 1 THEN 'A+'
        WHEN 2 THEN 'B+'
        ELSE 'AB+'
    END
FROM dbo.users u
WHERE u.email LIKE 'patient.test%@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM dbo.patients p WHERE p.user_id = u.id);
GO

-- ============================================================
--  2. TẠO LỊCH LÀM VIỆC + LỊCH HẸN + HỒ SƠ CHO BÁC SĨ TEST
--     (Toàn bộ phần này chạy trong 1 batch để dùng chung biến)
-- ============================================================

-- Lấy id của bác sĩ test và các mốc ngày
DECLARE @doctorId BIGINT = (
    SELECT d.id
    FROM dbo.doctors d
    JOIN dbo.users u ON u.id = d.user_id
    WHERE u.email = 'bs.nguyenvanan@medical.vn'
);

DECLARE @today DATE = CAST(GETDATE() AS DATE);
DECLARE @yesterday DATE = DATEADD(DAY, -1, @today);
DECLARE @dayBefore DATE = DATEADD(DAY, -2, @today);

-- 2a. Tạo ca làm EVENING cho 6 ngày (hôm kia .. 3 ngày tới)
--     Dùng ca EVENING để không đụng ca MORNING/AFTERNOON đã có sẵn.
;WITH work_dates AS (
    SELECT DATEADD(DAY, d.day_offset, @today) AS work_date
    FROM (VALUES (-2), (-1), (0), (1), (2), (3)) AS d(day_offset)
)
INSERT INTO dbo.work_schedules (doctor_id, work_date, shift, available)
SELECT @doctorId, work_dates.work_date, 'EVENING', 1
FROM work_dates
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.work_schedules ws
    WHERE ws.doctor_id = @doctorId
      AND ws.work_date = work_dates.work_date
      AND ws.shift = 'EVENING'
);

-- 2b. Tạo 3 khung giờ cho mỗi ca EVENING (sức chứa 30 để đủ chỗ cho dữ liệu test)
;WITH slot_template AS (
    SELECT CAST('18:00:00' AS TIME(0)) AS start_time, CAST('19:00:00' AS TIME(0)) AS end_time
    UNION ALL SELECT CAST('19:00:00' AS TIME(0)), CAST('20:00:00' AS TIME(0))
    UNION ALL SELECT CAST('20:00:00' AS TIME(0)), CAST('21:00:00' AS TIME(0))
)
INSERT INTO dbo.time_slots (schedule_id, start_time, end_time, booked_capacity, max_capacity, status)
SELECT ws.id, t.start_time, t.end_time, 0, 30, 'AVAILABLE'
FROM dbo.work_schedules ws
CROSS JOIN slot_template t
WHERE ws.doctor_id = @doctorId
  AND ws.shift = 'EVENING'
  AND NOT EXISTS (
      SELECT 1 FROM dbo.time_slots ts
      WHERE ts.schedule_id = ws.id
        AND ts.start_time = t.start_time
        AND ts.end_time = t.end_time
  );

-- 2c. Lịch hẹn HÔM KIA: cả 20 bệnh nhân đều ĐÃ KHÁM (để có lịch sử)
;WITH pts AS (
    SELECT p.id AS patient_id, ROW_NUMBER() OVER (ORDER BY p.id) AS rn
    FROM dbo.patients p
    JOIN dbo.users u ON u.id = p.user_id
    WHERE u.email LIKE 'patient.test%@gmail.com'
),
slots AS (
    SELECT ts.id AS slot_id, ROW_NUMBER() OVER (ORDER BY ts.start_time) AS sn
    FROM dbo.time_slots ts
    JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
    WHERE ws.doctor_id = @doctorId AND ws.work_date = @dayBefore AND ws.shift = 'EVENING'
)
INSERT INTO dbo.appointments (patient_id, doctor_id, time_slot_id, status, notes, created_at)
SELECT pts.patient_id, @doctorId, slots.slot_id, 'COMPLETED',
       N'Khám tổng quát (dữ liệu test)', DATEADD(DAY, -2, SYSDATETIME())
FROM pts
JOIN slots ON slots.sn = ((pts.rn - 1) % 3) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.appointments ex
    WHERE ex.patient_id = pts.patient_id AND ex.time_slot_id = slots.slot_id
);

-- 2d. Lịch hẹn HÔM QUA: 10 bệnh nhân đầu ĐÃ KHÁM (để vài bệnh nhân có nhiều lần khám)
;WITH pts AS (
    SELECT p.id AS patient_id, ROW_NUMBER() OVER (ORDER BY p.id) AS rn
    FROM dbo.patients p
    JOIN dbo.users u ON u.id = p.user_id
    WHERE u.email LIKE 'patient.test%@gmail.com'
),
slots AS (
    SELECT ts.id AS slot_id, ROW_NUMBER() OVER (ORDER BY ts.start_time) AS sn
    FROM dbo.time_slots ts
    JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
    WHERE ws.doctor_id = @doctorId AND ws.work_date = @yesterday AND ws.shift = 'EVENING'
)
INSERT INTO dbo.appointments (patient_id, doctor_id, time_slot_id, status, notes, created_at)
SELECT pts.patient_id, @doctorId, slots.slot_id, 'COMPLETED',
       N'Tái khám (dữ liệu test)', DATEADD(DAY, -1, SYSDATETIME())
FROM pts
JOIN slots ON slots.sn = ((pts.rn - 1) % 3) + 1
WHERE pts.rn <= 10
  AND NOT EXISTS (
      SELECT 1 FROM dbo.appointments ex
      WHERE ex.patient_id = pts.patient_id AND ex.time_slot_id = slots.slot_id
  );

-- 2e. Lịch hẹn HÔM NAY: 12 bệnh nhân với đủ 4 trạng thái
--     rn 1-4: Chờ xác nhận | 5-7: Đã xác nhận | 8-10: Đã khám | 11-12: Đã hủy
;WITH pts AS (
    SELECT p.id AS patient_id, ROW_NUMBER() OVER (ORDER BY p.id) AS rn
    FROM dbo.patients p
    JOIN dbo.users u ON u.id = p.user_id
    WHERE u.email LIKE 'patient.test%@gmail.com'
),
slots AS (
    SELECT ts.id AS slot_id, ROW_NUMBER() OVER (ORDER BY ts.start_time) AS sn
    FROM dbo.time_slots ts
    JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
    WHERE ws.doctor_id = @doctorId AND ws.work_date = @today AND ws.shift = 'EVENING'
)
INSERT INTO dbo.appointments (patient_id, doctor_id, time_slot_id, status, notes, created_at)
SELECT pts.patient_id, @doctorId, slots.slot_id,
       CASE
           WHEN pts.rn <= 4  THEN 'PENDING'
           WHEN pts.rn <= 7  THEN 'CONFIRMED'
           WHEN pts.rn <= 10 THEN 'COMPLETED'
           ELSE 'CANCELLED'
       END,
       N'Lịch khám hôm nay (dữ liệu test)', SYSDATETIME()
FROM pts
JOIN slots ON slots.sn = ((pts.rn - 1) % 3) + 1
WHERE pts.rn <= 12
  AND NOT EXISTS (
      SELECT 1 FROM dbo.appointments ex
      WHERE ex.patient_id = pts.patient_id AND ex.time_slot_id = slots.slot_id
  );

-- 2f. Tạo HỒ SƠ BỆNH ÁN cho mọi lịch hẹn ĐÃ KHÁM của bác sĩ test mà chưa có hồ sơ
INSERT INTO dbo.medical_records (appointment_id, doctor_id, patient_id, diagnosis, treatment, notes, created_at)
SELECT a.id, a.doctor_id, a.patient_id,
       N'Viêm họng cấp, theo dõi thêm',
       N'Nghỉ ngơi, uống nhiều nước ấm, giữ ấm cổ họng',
       N'Hẹn tái khám sau 5 ngày nếu chưa đỡ',
       a.created_at
FROM dbo.appointments a
WHERE a.doctor_id = @doctorId
  AND a.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM dbo.medical_records mr WHERE mr.appointment_id = a.id);

-- 2g. Kê 2 loại thuốc cho mỗi hồ sơ vừa tạo (nếu hồ sơ chưa có thuốc)
INSERT INTO dbo.prescriptions (medical_record_id, medicine_name, dosage, duration_days, instructions)
SELECT mr.id, N'Paracetamol 500mg', N'1 viên/lần, tối đa 3 lần/ngày', 3, N'Uống sau ăn khi sốt hoặc đau'
FROM dbo.medical_records mr
WHERE mr.doctor_id = @doctorId
  AND NOT EXISTS (SELECT 1 FROM dbo.prescriptions pr WHERE pr.medical_record_id = mr.id);

INSERT INTO dbo.prescriptions (medical_record_id, medicine_name, dosage, duration_days, instructions)
SELECT mr.id, N'Vitamin C 1000mg', N'1 viên/ngày', 7, N'Uống sau ăn sáng'
FROM dbo.medical_records mr
WHERE mr.doctor_id = @doctorId
  AND (SELECT COUNT(*) FROM dbo.prescriptions pr WHERE pr.medical_record_id = mr.id) < 2;

-- 2h. Đồng bộ lại số lượng đã đặt (booked_capacity) cho các khung giờ test
UPDATE ts
SET ts.booked_capacity = booked.count_booked
FROM dbo.time_slots ts
JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
CROSS APPLY (
    SELECT COUNT(*) AS count_booked
    FROM dbo.appointments a
    WHERE a.time_slot_id = ts.id AND a.status <> 'CANCELLED'
) booked
WHERE ws.doctor_id = @doctorId AND ws.shift = 'EVENING';
GO

-- ============================================================
--  3. KIỂM TRA NHANH KẾT QUẢ
-- ============================================================
PRINT N'--- Tổng số bệnh nhân test đã tạo ---';
SELECT COUNT(*) AS so_benh_nhan_test
FROM dbo.users WHERE email LIKE 'patient.test%@gmail.com';

PRINT N'--- Lịch hẹn HÔM NAY của bác sĩ test theo trạng thái ---';
SELECT a.status, COUNT(*) AS so_luong
FROM dbo.appointments a
JOIN dbo.doctors d ON d.id = a.doctor_id
JOIN dbo.users u ON u.id = d.user_id
JOIN dbo.time_slots ts ON ts.id = a.time_slot_id
JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
WHERE u.email = 'bs.nguyenvanan@medical.vn'
  AND ws.work_date = CAST(GETDATE() AS DATE)
GROUP BY a.status;

PRINT N'--- Tổng số hồ sơ bệnh án của bác sĩ test ---';
SELECT COUNT(*) AS so_ho_so
FROM dbo.medical_records mr
JOIN dbo.doctors d ON d.id = mr.doctor_id
JOIN dbo.users u ON u.id = d.user_id
WHERE u.email = 'bs.nguyenvanan@medical.vn';

PRINT N'>>> Xong. Đăng nhập test: SĐT 0901000002 / mật khẩu 123';
GO
