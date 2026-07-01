-- ============================================================
--  MEDICAL APPOINTMENT SYSTEM - SQL SERVER SCRIPT
--  Generated from Spring Boot entities (Java / JPA / Hibernate)
--  Database  : MedicalAppointmentSystem
--  Collation : Vietnamese_CI_AS
-- ============================================================

-- ============================================================
-- 0. CREATE DATABASE
-- ============================================================
USE master;
GO

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'MedicalAppointmentSystem')
BEGIN
    CREATE DATABASE MedicalAppointmentSystem
        COLLATE Vietnamese_CI_AS;
END
GO

USE MedicalAppointmentSystem;
GO

-- ============================================================
-- 1. DROP TABLES (reverse dependency order)
-- ============================================================
IF OBJECT_ID('test_results',   'U') IS NOT NULL DROP TABLE test_results;
IF OBJECT_ID('prescriptions',  'U') IS NOT NULL DROP TABLE prescriptions;
IF OBJECT_ID('medical_records','U') IS NOT NULL DROP TABLE medical_records;
IF OBJECT_ID('appointments',   'U') IS NOT NULL DROP TABLE appointments;
IF OBJECT_ID('work_schedules', 'U') IS NOT NULL DROP TABLE work_schedules;
IF OBJECT_ID('patients',       'U') IS NOT NULL DROP TABLE patients;
IF OBJECT_ID('doctors',        'U') IS NOT NULL DROP TABLE doctors;
IF OBJECT_ID('departments',    'U') IS NOT NULL DROP TABLE departments;
IF OBJECT_ID('user_roles',     'U') IS NOT NULL DROP TABLE user_roles;
IF OBJECT_ID('users',          'U') IS NOT NULL DROP TABLE users;
IF OBJECT_ID('roles',          'U') IS NOT NULL DROP TABLE roles;
GO

-- ============================================================
-- 2. TABLES
-- ============================================================

-- ----------------------------------------------------------
-- 2.1  roles
-- ----------------------------------------------------------
CREATE TABLE roles (
    id   BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(50)         NOT NULL,

    CONSTRAINT PK_roles      PRIMARY KEY (id),
    CONSTRAINT UQ_roles_name UNIQUE      (name),
    CONSTRAINT CK_roles_name CHECK       (name IN ('ROLE_PATIENT','ROLE_DOCTOR','ROLE_ADMIN'))
);
GO

-- ----------------------------------------------------------
-- 2.2  users
-- ----------------------------------------------------------
CREATE TABLE users (
    id         BIGINT         IDENTITY(1,1) NOT NULL,
    full_name  NVARCHAR(255)  NOT NULL,
    email      NVARCHAR(255)  NOT NULL,
    password   NVARCHAR(255)  NOT NULL,
    phone      NVARCHAR(20)   NULL,
    avatar_url NVARCHAR(500)  NULL,
    enabled    BIT            NOT NULL DEFAULT 1,

    CONSTRAINT PK_users       PRIMARY KEY (id),
    CONSTRAINT UQ_users_email UNIQUE      (email)
);
GO

-- ----------------------------------------------------------
-- 2.3  user_roles  (ManyToMany join table)
-- ----------------------------------------------------------
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    CONSTRAINT PK_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT FK_ur_user    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_ur_role    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);
GO

-- ----------------------------------------------------------
-- 2.4  departments
-- ----------------------------------------------------------
CREATE TABLE departments (
    id          BIGINT         IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(255)  NOT NULL,
    description NVARCHAR(MAX)  NULL,
    image_url   NVARCHAR(500)  NULL,

    CONSTRAINT PK_departments      PRIMARY KEY (id),
    CONSTRAINT UQ_departments_name UNIQUE      (name)
);
GO

-- ----------------------------------------------------------
-- 2.5  doctors
-- ----------------------------------------------------------
CREATE TABLE doctors (
    id               BIGINT         IDENTITY(1,1) NOT NULL,
    user_id          BIGINT         NOT NULL,
    department_id    BIGINT         NULL,
    specialization   NVARCHAR(255)  NOT NULL,
    bio              NVARCHAR(MAX)  NULL,
    experience_years INT            NULL,
    rating           FLOAT          NOT NULL DEFAULT 0.0,

    CONSTRAINT PK_doctors        PRIMARY KEY (id),
    CONSTRAINT UQ_doctors_user   UNIQUE      (user_id),
    CONSTRAINT FK_doctors_user   FOREIGN KEY (user_id)       REFERENCES users(id)       ON DELETE CASCADE,
    CONSTRAINT FK_doctors_dept   FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    CONSTRAINT CK_doctors_rating CHECK       (rating BETWEEN 0.0 AND 5.0)
);
GO

-- ----------------------------------------------------------
-- 2.6  patients
-- ----------------------------------------------------------
CREATE TABLE patients (
    id            BIGINT         IDENTITY(1,1) NOT NULL,
    user_id       BIGINT         NOT NULL,
    date_of_birth DATE           NULL,
    gender        NVARCHAR(10)   NULL,
    address       NVARCHAR(500)  NULL,
    blood_type    NVARCHAR(5)    NULL,

    CONSTRAINT PK_patients         PRIMARY KEY (id),
    CONSTRAINT UQ_patients_user    UNIQUE      (user_id),
    CONSTRAINT FK_patients_user    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT CK_patients_gender  CHECK       (gender IN ('MALE','FEMALE','OTHER')),
    CONSTRAINT CK_patients_blood   CHECK       (blood_type IN ('A','B','AB','O','A+','A-','B+','B-','AB+','AB-','O+','O-'))
);
GO

-- ----------------------------------------------------------
-- 2.7  work_schedules
-- ----------------------------------------------------------
CREATE TABLE work_schedules (
    id         BIGINT         IDENTITY(1,1) NOT NULL,
    doctor_id  BIGINT         NOT NULL,
    work_date  DATE           NOT NULL,
    start_time TIME(0)        NOT NULL,
    end_time   TIME(0)        NOT NULL,
    available  BIT            NOT NULL DEFAULT 1,
    shift      NVARCHAR(20)   NOT NULL DEFAULT 'MORNING',

    CONSTRAINT PK_work_schedules         PRIMARY KEY (id),
    CONSTRAINT FK_ws_doctor              FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    CONSTRAINT CK_ws_shift               CHECK       (shift IN ('MORNING','AFTERNOON','EVENING')),
    CONSTRAINT CK_ws_time                CHECK       (end_time > start_time)
);
GO

-- ----------------------------------------------------------
-- 2.8  appointments
-- ----------------------------------------------------------
CREATE TABLE appointments (
    id               BIGINT         IDENTITY(1,1) NOT NULL,
    patient_id       BIGINT         NOT NULL,
    doctor_id        BIGINT         NOT NULL,
    appointment_date DATE           NOT NULL,
    appointment_time TIME(0)        NOT NULL,
    status           NVARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    notes            NVARCHAR(MAX)  NULL,
    created_at       DATETIME2      NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT PK_appointments        PRIMARY KEY (id),
    CONSTRAINT FK_appt_patient        FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT FK_appt_doctor         FOREIGN KEY (doctor_id)  REFERENCES doctors(id),
    CONSTRAINT CK_appt_status         CHECK       (status IN ('PENDING','CONFIRMED','CANCELLED','COMPLETED'))
);
GO

-- ----------------------------------------------------------
-- 2.9  medical_records
-- ----------------------------------------------------------
CREATE TABLE medical_records (
    id             BIGINT         IDENTITY(1,1) NOT NULL,
    appointment_id BIGINT         NOT NULL,
    doctor_id      BIGINT         NOT NULL,
    patient_id     BIGINT         NOT NULL,
    diagnosis      NVARCHAR(MAX)  NOT NULL,
    treatment      NVARCHAR(MAX)  NULL,
    notes          NVARCHAR(MAX)  NULL,
    created_at     DATETIME2      NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT PK_medical_records       PRIMARY KEY (id),
    CONSTRAINT UQ_mr_appointment        UNIQUE      (appointment_id),
    CONSTRAINT FK_mr_appointment        FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT FK_mr_doctor             FOREIGN KEY (doctor_id)      REFERENCES doctors(id),
    CONSTRAINT FK_mr_patient            FOREIGN KEY (patient_id)     REFERENCES patients(id)
);
GO

-- ----------------------------------------------------------
-- 2.10  prescriptions
-- ----------------------------------------------------------
CREATE TABLE prescriptions (
    id                BIGINT         IDENTITY(1,1) NOT NULL,
    medical_record_id BIGINT         NOT NULL,
    medicine_name     NVARCHAR(255)  NOT NULL,
    dosage            NVARCHAR(255)  NOT NULL,
    duration_days     INT            NULL,
    instructions      NVARCHAR(500)  NULL,

    CONSTRAINT PK_prescriptions    PRIMARY KEY (id),
    CONSTRAINT FK_presc_mr         FOREIGN KEY (medical_record_id) REFERENCES medical_records(id) ON DELETE CASCADE,
    CONSTRAINT CK_presc_duration   CHECK       (duration_days IS NULL OR duration_days > 0)
);
GO

-- ----------------------------------------------------------
-- 2.11  test_results
-- ----------------------------------------------------------
CREATE TABLE test_results (
    id             BIGINT         IDENTITY(1,1) NOT NULL,
    appointment_id BIGINT         NOT NULL,
    patient_id     BIGINT         NOT NULL,
    file_name      NVARCHAR(255)  NOT NULL,
    file_url       NVARCHAR(500)  NOT NULL,
    description    NVARCHAR(500)  NULL,
    uploaded_at    DATETIME2      NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT PK_test_results   PRIMARY KEY (id),
    CONSTRAINT FK_tr_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT FK_tr_patient     FOREIGN KEY (patient_id)     REFERENCES patients(id)
);
GO

-- ============================================================
-- 3. INDEXES
-- ============================================================
CREATE INDEX IX_doctors_dept        ON doctors       (department_id);
CREATE INDEX IX_ws_doctor_date      ON work_schedules(doctor_id, work_date);
CREATE INDEX IX_ws_available        ON work_schedules(available, work_date);
CREATE INDEX IX_appt_patient        ON appointments  (patient_id);
CREATE INDEX IX_appt_doctor         ON appointments  (doctor_id);
CREATE INDEX IX_appt_date_status    ON appointments  (appointment_date, status);
CREATE INDEX IX_mr_patient          ON medical_records(patient_id);
CREATE INDEX IX_mr_doctor           ON medical_records(doctor_id);
CREATE INDEX IX_presc_mr            ON prescriptions  (medical_record_id);
CREATE INDEX IX_tr_appointment      ON test_results   (appointment_id);
CREATE INDEX IX_tr_patient          ON test_results   (patient_id);
GO

-- ============================================================
-- 4. SAMPLE DATA
-- ============================================================

-- ----------------------------------------------------------
-- 4.1  Roles
-- ----------------------------------------------------------
SET IDENTITY_INSERT roles ON;
INSERT INTO roles (id, name) VALUES
    (1, 'ROLE_ADMIN'),
    (2, 'ROLE_DOCTOR'),
    (3, 'ROLE_PATIENT');
SET IDENTITY_INSERT roles OFF;
GO

-- ----------------------------------------------------------
-- 4.2  Departments
-- ----------------------------------------------------------
INSERT INTO departments (name, description) VALUES
    (N'Khoa Nội', N'Khám và điều trị các bệnh lý nội khoa: tim mạch, tiêu hóa, hô hấp...'),
    (N'Khoa Ngoại', N'Phẫu thuật và điều trị các bệnh lý ngoại khoa'),
    (N'Khoa Nhi', N'Chăm sóc sức khỏe cho trẻ em từ sơ sinh đến 16 tuổi'),
    (N'Khoa Sản', N'Theo dõi thai kỳ, sinh đẻ và chăm sóc sau sinh'),
    (N'Khoa Da liễu', N'Khám và điều trị các bệnh về da, tóc, móng'),
    (N'Khoa Mắt', N'Khám, điều trị và phẫu thuật các bệnh về mắt'),
    (N'Khoa Tai Mũi Họng', N'Điều trị bệnh lý tai, mũi, họng và đầu mặt cổ'),
    (N'Khoa Xương Khớp', N'Điều trị các bệnh về cơ xương khớp'),
    (N'Khoa Thần Kinh', N'Chẩn đoán và điều trị bệnh lý thần kinh'),
    (N'Khoa Ung Bướu', N'Điều trị ung thư và các khối u');
GO

-- ----------------------------------------------------------
-- 4.3  Users (password = BCrypt of "Password@123")
--       $2a$10$... is a placeholder — replace with real BCrypt hash
-- ----------------------------------------------------------
DECLARE @bcrypt NVARCHAR(255) = N'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh';

INSERT INTO users (full_name, email, password, phone, enabled) VALUES
    -- Admin
    (N'Admin Hệ Thống',      'admin@medical.vn',       @bcrypt, '0901000001', 1),
    -- Doctors
    (N'BS. Nguyễn Văn An',   'bs.nguyenvanan@medical.vn',   @bcrypt, '0901000002', 1),
    (N'BS. Trần Thị Bình',   'bs.tranthiminh@medical.vn',   @bcrypt, '0901000003', 1),
    (N'BS. Lê Quốc Cường',   'bs.lequoccuong@medical.vn',   @bcrypt, '0901000004', 1),
    -- Patients
    (N'Phạm Minh Đức',       'duc.pham@gmail.com',     @bcrypt, '0901000005', 1),
    (N'Hoàng Thị Em',        'em.hoang@gmail.com',     @bcrypt, '0901000006', 1);
GO

-- Assign roles
-- Admin (user_id = 1)
INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u CROSS JOIN roles r
    WHERE u.email = 'admin@medical.vn' AND r.name = 'ROLE_ADMIN';

-- Doctors (user_id = 2,3,4)
INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u CROSS JOIN roles r
    WHERE u.email IN ('bs.nguyenvanan@medical.vn','bs.tranthiminh@medical.vn','bs.lequoccuong@medical.vn')
      AND r.name = 'ROLE_DOCTOR';

-- Patients (user_id = 5,6)
INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u CROSS JOIN roles r
    WHERE u.email IN ('duc.pham@gmail.com','em.hoang@gmail.com')
      AND r.name = 'ROLE_PATIENT';
GO

-- ----------------------------------------------------------
-- 4.4  Doctors
-- ----------------------------------------------------------
INSERT INTO doctors (user_id, department_id, specialization, bio, experience_years, rating)
SELECT
    u.id,
    d.id,
    v.spec,
    v.bio,
    v.exp,
    v.rating
FROM (VALUES
    ('bs.nguyenvanan@medical.vn',   N'Khoa Nội',       N'Tim mạch – Nội khoa tổng quát', N'Bác sĩ chuyên khoa II, hơn 15 năm kinh nghiệm điều trị bệnh tim mạch và nội khoa.', 15, 4.8),
    ('bs.tranthiminh@medical.vn',   N'Khoa Nhi',       N'Nhi khoa',                       N'Thạc sĩ bác sĩ, chuyên gia về sức khỏe trẻ em và dinh dưỡng nhi.', 10, 4.7),
    ('bs.lequoccuong@medical.vn',   N'Khoa Da liễu',   N'Da liễu – Thẩm mỹ da',           N'Bác sĩ chuyên khoa I, kinh nghiệm điều trị các bệnh da liễu phức tạp.', 8, 4.6)
) AS v(email, dept_name, spec, bio, exp, rating)
JOIN users       u ON u.email    = v.email
JOIN departments d ON d.name     = v.dept_name;
GO

-- ----------------------------------------------------------
-- 4.5  Patients
-- ----------------------------------------------------------
INSERT INTO patients (user_id, date_of_birth, gender, address, blood_type)
SELECT u.id, v.dob, v.gender, v.addr, v.blood
FROM (VALUES
    ('duc.pham@gmail.com', '1990-05-15', 'MALE',   N'123 Lê Lợi, Quận 1, TP.HCM',     'O+'),
    ('em.hoang@gmail.com', '1995-11-20', 'FEMALE', N'456 Nguyễn Huệ, Quận 3, TP.HCM', 'A+')
) AS v(email, dob, gender, addr, blood)
JOIN users u ON u.email = v.email;
GO

-- ----------------------------------------------------------
-- 4.6  Work Schedules (next 7 days for each doctor)
-- ----------------------------------------------------------
INSERT INTO work_schedules (doctor_id, work_date, start_time, end_time, available, shift)
SELECT
    doc.id,
    CAST(DATEADD(DAY, offset_days, CAST(GETDATE() AS DATE)) AS DATE),
    CAST(s.start_t AS TIME(0)),
    CAST(s.end_t   AS TIME(0)),
    1,
    s.shift_name
FROM (
    SELECT 1 AS offset_days UNION SELECT 2 UNION SELECT 3
    UNION SELECT 4 UNION SELECT 5 UNION SELECT 7
) AS days(offset_days)
CROSS JOIN (
    VALUES
        ('07:30','11:30','MORNING'),
        ('13:30','17:00','AFTERNOON')
) AS s(start_t, end_t, shift_name)
CROSS JOIN doctors doc;
GO

-- ----------------------------------------------------------
-- 4.7  Appointments (sample)
-- ----------------------------------------------------------
DECLARE @patient1 BIGINT = (SELECT id FROM patients WHERE user_id = (SELECT id FROM users WHERE email = 'duc.pham@gmail.com'));
DECLARE @patient2 BIGINT = (SELECT id FROM patients WHERE user_id = (SELECT id FROM users WHERE email = 'em.hoang@gmail.com'));
DECLARE @doctor1  BIGINT = (SELECT id FROM doctors  WHERE user_id = (SELECT id FROM users WHERE email = 'bs.nguyenvanan@medical.vn'));
DECLARE @doctor2  BIGINT = (SELECT id FROM doctors  WHERE user_id = (SELECT id FROM users WHERE email = 'bs.tranthiminh@medical.vn'));

INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, status, notes, created_at)
VALUES
    (@patient1, @doctor1, DATEADD(DAY,  1, CAST(GETDATE() AS DATE)), '09:00', 'CONFIRMED',  N'Bệnh nhân bị đau ngực và khó thở',               SYSDATETIME()),
    (@patient2, @doctor2, DATEADD(DAY,  2, CAST(GETDATE() AS DATE)), '14:00', 'PENDING',    N'Trẻ bị sốt cao 3 ngày, không ăn được',           SYSDATETIME()),
    (@patient1, @doctor2, DATEADD(DAY, -5, CAST(GETDATE() AS DATE)), '10:00', 'COMPLETED',  N'Kiểm tra sức khỏe định kỳ',                      SYSDATETIME()),
    (@patient2, @doctor1, DATEADD(DAY, -3, CAST(GETDATE() AS DATE)), '08:30', 'CANCELLED',  N'Bệnh nhân hủy do bận công việc',                 SYSDATETIME());
GO

-- ----------------------------------------------------------
-- 4.8  Medical Record + Prescriptions (for COMPLETED appointment)
-- ----------------------------------------------------------
DECLARE @completed_appt BIGINT = (
    SELECT TOP 1 a.id
    FROM appointments a
    WHERE a.status = 'COMPLETED'
    ORDER BY a.id
);
DECLARE @dr_id  BIGINT = (SELECT doctor_id  FROM appointments WHERE id = @completed_appt);
DECLARE @pat_id BIGINT = (SELECT patient_id FROM appointments WHERE id = @completed_appt);

INSERT INTO medical_records (appointment_id, doctor_id, patient_id, diagnosis, treatment, notes, created_at)
VALUES (
    @completed_appt,
    @dr_id,
    @pat_id,
    N'Viêm họng cấp tính, sốt nhẹ 37.8°C',
    N'Nghỉ ngơi, uống nhiều nước. Dùng thuốc theo đơn.',
    N'Tái khám sau 7 ngày nếu không đỡ.',
    SYSDATETIME()
);

DECLARE @mr_id BIGINT = SCOPE_IDENTITY();

INSERT INTO prescriptions (medical_record_id, medicine_name, dosage, duration_days, instructions)
VALUES
    (@mr_id, N'Paracetamol 500mg', N'1 viên / lần × 3 lần/ngày',  5, N'Uống sau ăn, không quá 3g/ngày'),
    (@mr_id, N'Amoxicillin 500mg', N'1 viên / lần × 2 lần/ngày',  7, N'Uống đúng giờ, không bỏ liều'),
    (@mr_id, N'Vitamin C 1000mg',  N'1 viên / ngày',               14, N'Uống sau bữa ăn sáng');
GO

-- ============================================================
-- 5. VIEWS
-- ============================================================

-- ----------------------------------------------------------
-- 5.1  Appointment details (join all)
-- ----------------------------------------------------------
CREATE OR ALTER VIEW vw_appointment_details AS
SELECT
    a.id                                          AS appointment_id,
    a.appointment_date,
    a.appointment_time,
    a.status,
    a.notes                                       AS appointment_notes,
    a.created_at,
    -- Patient info
    pu.full_name                                  AS patient_name,
    pu.email                                      AS patient_email,
    pu.phone                                      AS patient_phone,
    p.gender,
    p.date_of_birth,
    p.blood_type,
    -- Doctor info
    du.full_name                                  AS doctor_name,
    du.email                                      AS doctor_email,
    doc.specialization,
    doc.rating,
    -- Department
    dep.name                                      AS department_name
FROM appointments a
JOIN patients    p   ON p.id            = a.patient_id
JOIN users       pu  ON pu.id           = p.user_id
JOIN doctors     doc ON doc.id          = a.doctor_id
JOIN users       du  ON du.id           = doc.user_id
LEFT JOIN departments dep ON dep.id     = doc.department_id;
GO

-- ----------------------------------------------------------
-- 5.2  Available work schedules
-- ----------------------------------------------------------
CREATE OR ALTER VIEW vw_available_schedules AS
SELECT
    ws.id                   AS schedule_id,
    ws.work_date,
    ws.start_time,
    ws.end_time,
    ws.shift,
    du.full_name            AS doctor_name,
    doc.specialization,
    dep.name                AS department_name,
    doc.rating
FROM work_schedules ws
JOIN doctors      doc ON doc.id  = ws.doctor_id
JOIN users        du  ON du.id   = doc.user_id
LEFT JOIN departments dep ON dep.id = doc.department_id
WHERE ws.available = 1
  AND ws.work_date >= CAST(GETDATE() AS DATE);
GO

-- ----------------------------------------------------------
-- 5.3  Full medical record view
-- ----------------------------------------------------------
CREATE OR ALTER VIEW vw_medical_records_full AS
SELECT
    mr.id                   AS record_id,
    mr.created_at,
    mr.diagnosis,
    mr.treatment,
    mr.notes,
    -- Appointment
    a.appointment_date,
    a.appointment_time,
    -- Patient
    pu.full_name            AS patient_name,
    p.date_of_birth,
    p.gender,
    p.blood_type,
    -- Doctor
    du.full_name            AS doctor_name,
    doc.specialization,
    dep.name                AS department_name
FROM medical_records mr
JOIN appointments  a   ON a.id   = mr.appointment_id
JOIN patients      p   ON p.id   = mr.patient_id
JOIN users         pu  ON pu.id  = p.user_id
JOIN doctors       doc ON doc.id = mr.doctor_id
JOIN users         du  ON du.id  = doc.user_id
LEFT JOIN departments dep ON dep.id = doc.department_id;
GO

-- ============================================================
-- 6. STORED PROCEDURES
-- ============================================================

-- ----------------------------------------------------------
-- 6.1  Book appointment
-- ----------------------------------------------------------
CREATE OR ALTER PROCEDURE sp_book_appointment
    @patient_id       BIGINT,
    @doctor_id        BIGINT,
    @appointment_date DATE,
    @appointment_time TIME(0),
    @notes            NVARCHAR(MAX) = NULL,
    @new_id           BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    -- Check duplicate booking
    IF EXISTS (
        SELECT 1 FROM appointments
        WHERE doctor_id        = @doctor_id
          AND appointment_date = @appointment_date
          AND appointment_time = @appointment_time
          AND status NOT IN ('CANCELLED')
    )
    BEGIN
        RAISERROR(N'Lịch hẹn này đã có người đặt. Vui lòng chọn thời gian khác.', 16, 1);
        RETURN;
    END;

    INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, status, notes, created_at)
    VALUES (@patient_id, @doctor_id, @appointment_date, @appointment_time, 'PENDING', @notes, SYSDATETIME());

    SET @new_id = SCOPE_IDENTITY();
END;
GO

-- ----------------------------------------------------------
-- 6.2  Update appointment status
-- ----------------------------------------------------------
CREATE OR ALTER PROCEDURE sp_update_appointment_status
    @appointment_id BIGINT,
    @new_status     NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;

    IF @new_status NOT IN ('PENDING','CONFIRMED','CANCELLED','COMPLETED')
    BEGIN
        RAISERROR(N'Trạng thái không hợp lệ.', 16, 1);
        RETURN;
    END;

    UPDATE appointments
    SET    status = @new_status
    WHERE  id     = @appointment_id;

    IF @@ROWCOUNT = 0
        RAISERROR(N'Không tìm thấy lịch hẹn.', 16, 1);
END;
GO

-- ----------------------------------------------------------
-- 6.3  Dashboard statistics
-- ----------------------------------------------------------
CREATE OR ALTER PROCEDURE sp_dashboard_stats
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        (SELECT COUNT(*) FROM appointments WHERE CAST(created_at AS DATE) = CAST(GETDATE() AS DATE))  AS appointments_today,
        (SELECT COUNT(*) FROM appointments WHERE status = 'PENDING')                                  AS pending_appointments,
        (SELECT COUNT(*) FROM appointments WHERE status = 'CONFIRMED')                                AS confirmed_appointments,
        (SELECT COUNT(*) FROM appointments WHERE status = 'COMPLETED')                                AS completed_appointments,
        (SELECT COUNT(*) FROM patients)                                                               AS total_patients,
        (SELECT COUNT(*) FROM doctors)                                                                AS total_doctors,
        (SELECT COUNT(*) FROM departments)                                                            AS total_departments,
        (SELECT COUNT(*) FROM medical_records)                                                        AS total_medical_records;
END;
GO

-- ============================================================
-- 7. QUICK VERIFICATION QUERIES
-- ============================================================

-- Uncomment to verify after running:
-- SELECT * FROM roles;
-- SELECT * FROM departments;
-- SELECT * FROM vw_appointment_details;
-- SELECT * FROM vw_available_schedules;
-- EXEC sp_dashboard_stats;

PRINT N'✅  Database MedicalAppointmentSystem khởi tạo thành công!';
GO
