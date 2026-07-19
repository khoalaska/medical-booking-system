-- ============================================================
--  MEDICAL BOOKING SYSTEM - FULL REBUILD SCRIPT
--  Schema version: work_schedules + time_slots
--  Warning: this script drops and recreates all main tables.
-- ============================================================

USE master;
GO

IF NOT EXISTS (SELECT 1 FROM sys.databases WHERE name = N'MedicalAppointmentSystem')
BEGIN
    CREATE DATABASE MedicalAppointmentSystem COLLATE Vietnamese_CI_AS;
END
GO

USE MedicalAppointmentSystem;
GO

-- ============================================================
--  1. DROP OLD OBJECTS
-- ============================================================

IF OBJECT_ID('dbo.vw_appointment_details', 'V') IS NOT NULL DROP VIEW dbo.vw_appointment_details;
IF OBJECT_ID('dbo.vw_available_time_slots', 'V') IS NOT NULL DROP VIEW dbo.vw_available_time_slots;
IF OBJECT_ID('dbo.vw_medical_records_full', 'V') IS NOT NULL DROP VIEW dbo.vw_medical_records_full;
GO

IF OBJECT_ID('dbo.sp_book_appointment', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_book_appointment;
IF OBJECT_ID('dbo.sp_cancel_appointment', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_cancel_appointment;
IF OBJECT_ID('dbo.sp_dashboard_stats', 'P') IS NOT NULL DROP PROCEDURE dbo.sp_dashboard_stats;
GO

IF OBJECT_ID('dbo.file_uploads', 'U') IS NOT NULL DROP TABLE dbo.file_uploads;
IF OBJECT_ID('dbo.test_results', 'U') IS NOT NULL DROP TABLE dbo.test_results;
IF OBJECT_ID('dbo.prescriptions', 'U') IS NOT NULL DROP TABLE dbo.prescriptions;
IF OBJECT_ID('dbo.medical_records', 'U') IS NOT NULL DROP TABLE dbo.medical_records;
IF OBJECT_ID('dbo.appointments', 'U') IS NOT NULL DROP TABLE dbo.appointments;
IF OBJECT_ID('dbo.time_slots', 'U') IS NOT NULL DROP TABLE dbo.time_slots;
IF OBJECT_ID('dbo.work_schedules', 'U') IS NOT NULL DROP TABLE dbo.work_schedules;
IF OBJECT_ID('dbo.patients', 'U') IS NOT NULL DROP TABLE dbo.patients;
IF OBJECT_ID('dbo.doctors', 'U') IS NOT NULL DROP TABLE dbo.doctors;
IF OBJECT_ID('dbo.departments', 'U') IS NOT NULL DROP TABLE dbo.departments;
IF OBJECT_ID('dbo.user_roles', 'U') IS NOT NULL DROP TABLE dbo.user_roles;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;
IF OBJECT_ID('dbo.roles', 'U') IS NOT NULL DROP TABLE dbo.roles;
GO

-- ============================================================
--  2. CREATE TABLES
-- ============================================================

CREATE TABLE dbo.roles (
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(50) NOT NULL,

    CONSTRAINT PK_roles PRIMARY KEY (id),
    CONSTRAINT UQ_roles_name UNIQUE (name),
    CONSTRAINT CK_roles_name CHECK (name IN ('ROLE_PATIENT', 'ROLE_DOCTOR', 'ROLE_ADMIN'))
);
GO

CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1,1) NOT NULL,
    full_name NVARCHAR(255) NOT NULL,
    email NVARCHAR(255) NOT NULL,
    password NVARCHAR(255) NOT NULL,
    phone NVARCHAR(20) NULL,
    avatar_url NVARCHAR(500) NULL,
    enabled BIT NOT NULL DEFAULT 1,

    CONSTRAINT PK_users PRIMARY KEY (id),
    CONSTRAINT UQ_users_email UNIQUE (email)
);
GO

CREATE TABLE dbo.user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    CONSTRAINT PK_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT FK_user_roles_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT FK_user_roles_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.departments (
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    image_url NVARCHAR(500) NULL,

    CONSTRAINT PK_departments PRIMARY KEY (id),
    CONSTRAINT UQ_departments_name UNIQUE (name)
);
GO

CREATE TABLE dbo.doctors (
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    department_id BIGINT NULL,
    specialization NVARCHAR(255) NOT NULL,
    bio NVARCHAR(MAX) NULL,
    experience_years INT NULL,
    rating FLOAT NOT NULL DEFAULT 0.0,

    CONSTRAINT PK_doctors PRIMARY KEY (id),
    CONSTRAINT UQ_doctors_user UNIQUE (user_id),
    CONSTRAINT FK_doctors_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT FK_doctors_department FOREIGN KEY (department_id) REFERENCES dbo.departments(id) ON DELETE SET NULL,
    CONSTRAINT CK_doctors_rating CHECK (rating BETWEEN 0.0 AND 5.0)
);
GO

CREATE TABLE dbo.patients (
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    date_of_birth DATE NULL,
    gender NVARCHAR(10) NULL,
    address NVARCHAR(500) NULL,
    blood_type NVARCHAR(5) NULL,

    CONSTRAINT PK_patients PRIMARY KEY (id),
    CONSTRAINT UQ_patients_user UNIQUE (user_id),
    CONSTRAINT FK_patients_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT CK_patients_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT CK_patients_blood CHECK (blood_type IN ('A', 'B', 'AB', 'O', 'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'))
);
GO

-- work_schedules only stores doctor + date + shift.
-- Time ranges and capacities are moved to time_slots.
CREATE TABLE dbo.work_schedules (
    id BIGINT IDENTITY(1,1) NOT NULL,
    doctor_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    shift NVARCHAR(20) NOT NULL DEFAULT 'MORNING',
    available BIT NOT NULL DEFAULT 1,

    CONSTRAINT PK_work_schedules PRIMARY KEY (id),
    CONSTRAINT FK_work_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES dbo.doctors(id) ON DELETE CASCADE,
    CONSTRAINT CK_work_schedules_shift CHECK (shift IN ('MORNING', 'AFTERNOON', 'EVENING')),
    CONSTRAINT UQ_work_schedules_doctor_date_shift UNIQUE (doctor_id, work_date, shift)
);
GO

CREATE TABLE dbo.time_slots (
    id BIGINT IDENTITY(1,1) NOT NULL,
    schedule_id BIGINT NOT NULL,
    start_time TIME(0) NOT NULL,
    end_time TIME(0) NOT NULL,
    booked_capacity INT NOT NULL DEFAULT 0,
    max_capacity INT NOT NULL DEFAULT 3,
    status NVARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',

    CONSTRAINT PK_time_slots PRIMARY KEY (id),
    CONSTRAINT FK_time_slots_schedule FOREIGN KEY (schedule_id) REFERENCES dbo.work_schedules(id) ON DELETE CASCADE,
    CONSTRAINT CK_time_slots_time CHECK (end_time > start_time),
    CONSTRAINT CK_time_slots_capacity CHECK (
        booked_capacity >= 0
        AND max_capacity > 0
        AND booked_capacity <= max_capacity
    ),
    CONSTRAINT CK_time_slots_status CHECK (status IN ('AVAILABLE', 'FULL', 'UNAVAILABLE')),
    CONSTRAINT UQ_time_slots_schedule_time UNIQUE (schedule_id, start_time, end_time)
);
GO

CREATE TABLE dbo.appointments (
    id BIGINT IDENTITY(1,1) NOT NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    time_slot_id BIGINT NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes NVARCHAR(MAX) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT PK_appointments PRIMARY KEY (id),
    CONSTRAINT FK_appointments_patient FOREIGN KEY (patient_id) REFERENCES dbo.patients(id),
    CONSTRAINT FK_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES dbo.doctors(id),
    CONSTRAINT FK_appointments_time_slot FOREIGN KEY (time_slot_id) REFERENCES dbo.time_slots(id),
    CONSTRAINT CK_appointments_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED'))
);
GO

CREATE TABLE dbo.medical_records (
    id BIGINT IDENTITY(1,1) NOT NULL,
    appointment_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    diagnosis NVARCHAR(MAX) NOT NULL,
    treatment NVARCHAR(MAX) NULL,
    notes NVARCHAR(MAX) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT PK_medical_records PRIMARY KEY (id),
    CONSTRAINT UQ_medical_records_appointment UNIQUE (appointment_id),
    CONSTRAINT FK_medical_records_appointment FOREIGN KEY (appointment_id) REFERENCES dbo.appointments(id),
    CONSTRAINT FK_medical_records_doctor FOREIGN KEY (doctor_id) REFERENCES dbo.doctors(id),
    CONSTRAINT FK_medical_records_patient FOREIGN KEY (patient_id) REFERENCES dbo.patients(id)
);
GO

CREATE TABLE dbo.prescriptions (
    id BIGINT IDENTITY(1,1) NOT NULL,
    medical_record_id BIGINT NOT NULL,
    medicine_name NVARCHAR(255) NOT NULL,
    dosage NVARCHAR(255) NOT NULL,
    duration_days INT NULL,
    instructions NVARCHAR(500) NULL,

    CONSTRAINT PK_prescriptions PRIMARY KEY (id),
    CONSTRAINT FK_prescriptions_medical_record FOREIGN KEY (medical_record_id) REFERENCES dbo.medical_records(id) ON DELETE CASCADE,
    CONSTRAINT CK_prescriptions_duration CHECK (duration_days IS NULL OR duration_days > 0)
);
GO

-- Replaces old test_results and matches the FileUpload requirement better.
CREATE TABLE dbo.file_uploads (
    id BIGINT IDENTITY(1,1) NOT NULL,
    uploaded_by_user_id BIGINT NULL,
    appointment_id BIGINT NULL,
    patient_id BIGINT NULL,
    doctor_id BIGINT NULL,
    file_name NVARCHAR(255) NOT NULL,
    file_url NVARCHAR(500) NOT NULL,
    file_type NVARCHAR(50) NOT NULL DEFAULT 'MEDICAL_DOCUMENT',
    description NVARCHAR(500) NULL,
    uploaded_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT PK_file_uploads PRIMARY KEY (id),
    CONSTRAINT FK_file_uploads_user FOREIGN KEY (uploaded_by_user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_file_uploads_appointment FOREIGN KEY (appointment_id) REFERENCES dbo.appointments(id),
    CONSTRAINT FK_file_uploads_patient FOREIGN KEY (patient_id) REFERENCES dbo.patients(id),
    CONSTRAINT FK_file_uploads_doctor FOREIGN KEY (doctor_id) REFERENCES dbo.doctors(id),
    CONSTRAINT CK_file_uploads_type CHECK (file_type IN ('TEST_RESULT', 'DOCTOR_AVATAR', 'MEDICAL_DOCUMENT', 'OTHER'))
);
GO

-- ============================================================
--  3. INDEXES
-- ============================================================

CREATE INDEX IX_doctors_department ON dbo.doctors(department_id);
CREATE INDEX IX_work_schedules_doctor_date ON dbo.work_schedules(doctor_id, work_date);
CREATE INDEX IX_work_schedules_available ON dbo.work_schedules(available, work_date);
CREATE INDEX IX_time_slots_schedule_status ON dbo.time_slots(schedule_id, status);
CREATE INDEX IX_appointments_patient ON dbo.appointments(patient_id);
CREATE INDEX IX_appointments_doctor ON dbo.appointments(doctor_id);
CREATE INDEX IX_appointments_time_slot ON dbo.appointments(time_slot_id);
CREATE INDEX IX_appointments_status ON dbo.appointments(status);
CREATE UNIQUE INDEX UX_appointments_patient_time_slot_active
ON dbo.appointments(patient_id, time_slot_id)
WHERE status <> 'CANCELLED';
CREATE INDEX IX_medical_records_patient ON dbo.medical_records(patient_id);
CREATE INDEX IX_medical_records_doctor ON dbo.medical_records(doctor_id);
CREATE INDEX IX_prescriptions_medical_record ON dbo.prescriptions(medical_record_id);
CREATE INDEX IX_file_uploads_appointment ON dbo.file_uploads(appointment_id);
CREATE INDEX IX_file_uploads_patient ON dbo.file_uploads(patient_id);
GO

-- ============================================================
--  4. SAMPLE DATA
-- ============================================================

INSERT INTO dbo.roles(name)
VALUES ('ROLE_ADMIN'), ('ROLE_DOCTOR'), ('ROLE_PATIENT');
GO

INSERT INTO dbo.departments(name, description)
VALUES
    (N'Khoa Nội', N'Khám và điều trị các bệnh lý nội khoa: tim mạch, tiêu hóa, hô hấp.'),
    (N'Khoa Ngoại', N'Phẫu thuật và điều trị các bệnh lý ngoại khoa.'),
    (N'Khoa Nhi', N'Chăm sóc sức khỏe cho trẻ em từ sơ sinh đến 16 tuổi.'),
    (N'Khoa Sản', N'Theo dõi thai kỳ, sinh đẻ và chăm sóc sau sinh.'),
    (N'Khoa Da liễu', N'Khám và điều trị các bệnh về da, tóc, móng.'),
    (N'Khoa Mắt', N'Khám, điều trị và phẫu thuật các bệnh về mắt.'),
    (N'Khoa Tai Mũi Họng', N'Điều trị bệnh lý tai, mũi, họng và đầu mặt cổ.'),
    (N'Khoa Xương Khớp', N'Điều trị các bệnh về cơ xương khớp.');
GO

DECLARE @bcrypt NVARCHAR(255) = N'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh';

INSERT INTO dbo.users(full_name, email, password, phone, enabled)
VALUES
    (N'Admin Hệ Thống', 'admin@medical.vn', @bcrypt, '0901000001', 1),

    (N'BS. Nguyễn Văn An', 'bs.nguyenvanan@medical.vn', @bcrypt, '0901000002', 1),
    (N'BS. Trần Minh Khang', 'bs.tranminhkhang@medical.vn', @bcrypt, '0902000001', 1),
    (N'BS. Lê Minh Hoàng', 'bs.leminhhoang@medical.vn', @bcrypt, '0902000013', 1),
    (N'BS. Phạm Đức Huy', 'bs.phamduchuy@medical.vn', @bcrypt, '0902000002', 1),
    (N'BS. Trần Thị Bình', 'bs.tranthiminh@medical.vn', @bcrypt, '0901000003', 1),
    (N'BS. Nguyễn Thảo Linh', 'bs.nguyenthaolinh@medical.vn', @bcrypt, '0902000003', 1),
    (N'BS. Hoàng Thu Hà', 'bs.hoangthuha@medical.vn', @bcrypt, '0902000004', 1),
    (N'BS. Đỗ Minh Ngọc', 'bs.dominhngoc@medical.vn', @bcrypt, '0902000005', 1),
    (N'BS. Lê Quốc Cường', 'bs.lequoccuong@medical.vn', @bcrypt, '0901000004', 1),
    (N'BS. Vũ Hải Yến', 'bs.vuhaiyen@medical.vn', @bcrypt, '0902000006', 1),
    (N'BS. Phan Quang Minh', 'bs.phanquangminh@medical.vn', @bcrypt, '0902000007', 1),
    (N'BS. Mai Thanh Hương', 'bs.maithanhhuong@medical.vn', @bcrypt, '0902000008', 1),
    (N'BS. Đặng Nhật Nam', 'bs.dangnhatnam@medical.vn', @bcrypt, '0902000009', 1),
    (N'BS. Bùi Khánh Vy', 'bs.buikhanhvy@medical.vn', @bcrypt, '0902000010', 1),
    (N'BS. Võ Anh Tuấn', 'bs.voanhtuan@medical.vn', @bcrypt, '0902000011', 1),
    (N'BS. Nguyễn Gia Bảo', 'bs.nguyengiabao@medical.vn', @bcrypt, '0902000012', 1),

    (N'Phạm Minh Đức', 'duc.pham@gmail.com', @bcrypt, '0901000005', 1),
    (N'Hoàng Thị Em', 'em.hoang@gmail.com', @bcrypt, '0901000006', 1),
    (N'Nguyễn Hải Nam', 'nam.nguyen@gmail.com', @bcrypt, '0901000007', 1);
GO

INSERT INTO dbo.user_roles(user_id, role_id)
SELECT u.id, r.id
FROM dbo.users u
JOIN dbo.roles r ON
    (u.email = 'admin@medical.vn' AND r.name = 'ROLE_ADMIN')
    OR (u.email LIKE 'bs.%@medical.vn' AND r.name = 'ROLE_DOCTOR')
    OR (u.email IN ('duc.pham@gmail.com', 'em.hoang@gmail.com', 'nam.nguyen@gmail.com') AND r.name = 'ROLE_PATIENT');
GO

INSERT INTO dbo.doctors(user_id, department_id, specialization, bio, experience_years, rating)
SELECT u.id, dep.id, v.specialization, v.bio, v.experience_years, v.rating
FROM (VALUES
    ('bs.nguyenvanan@medical.vn', N'Khoa Nội', N'Nội tổng quát, tim mạch', N'Bác sĩ chuyên khoa II, nhiều kinh nghiệm điều trị bệnh nội khoa và tim mạch.', 15, 4.8),
    ('bs.tranminhkhang@medical.vn', N'Khoa Nội', N'Tiêu hóa, hô hấp', N'Bác sĩ chuyên khám các bệnh tiêu hóa, hô hấp và nội khoa thường gặp.', 9, 4.5),
    ('bs.leminhhoang@medical.vn', N'Khoa Ngoại', N'Ngoại tổng quát', N'Bác sĩ chuyên phẫu thuật và điều trị các bệnh ngoại khoa.', 12, 4.6),
    ('bs.phamduchuy@medical.vn', N'Khoa Ngoại', N'Ngoại tiêu hóa', N'Bác sĩ có kinh nghiệm trong khám và tư vấn ngoại tiêu hóa.', 8, 4.4),
    ('bs.tranthiminh@medical.vn', N'Khoa Nhi', N'Nhi khoa tổng quát', N'Bác sĩ chuyên khám và điều trị bệnh thường gặp ở trẻ em.', 10, 4.7),
    ('bs.nguyenthaolinh@medical.vn', N'Khoa Nhi', N'Dinh dưỡng nhi, sốt trẻ em', N'Bác sĩ chuyên tư vấn dinh dưỡng và điều trị bệnh thường gặp ở trẻ nhỏ.', 7, 4.5),
    ('bs.hoangthuha@medical.vn', N'Khoa Sản', N'Sản phụ khoa', N'Bác sĩ chuyên khám thai, tư vấn thai kỳ và chăm sóc sức khỏe phụ nữ.', 11, 4.7),
    ('bs.dominhngoc@medical.vn', N'Khoa Sản', N'Thai kỳ, chăm sóc sau sinh', N'Bác sĩ chuyên theo dõi thai kỳ và chăm sóc mẹ sau sinh.', 6, 4.4),
    ('bs.lequoccuong@medical.vn', N'Khoa Da liễu', N'Da liễu, thẩm mỹ da', N'Bác sĩ chuyên điều trị mụn, viêm da và các bệnh da liễu phổ biến.', 8, 4.6),
    ('bs.vuhaiyen@medical.vn', N'Khoa Da liễu', N'Dị ứng da, mụn, viêm da', N'Bác sĩ chuyên điều trị mụn, dị ứng và viêm da.', 5, 4.3),
    ('bs.phanquangminh@medical.vn', N'Khoa Mắt', N'Khám mắt tổng quát', N'Bác sĩ chuyên khám mắt tổng quát và tư vấn chăm sóc mắt.', 9, 4.5),
    ('bs.maithanhhuong@medical.vn', N'Khoa Mắt', N'Tật khúc xạ, mắt trẻ em', N'Bác sĩ chuyên khám tật khúc xạ và bệnh mắt trẻ em.', 6, 4.4),
    ('bs.dangnhatnam@medical.vn', N'Khoa Tai Mũi Họng', N'Tai mũi họng tổng quát', N'Bác sĩ chuyên điều trị viêm họng, viêm mũi và bệnh tai mũi họng.', 10, 4.6),
    ('bs.buikhanhvy@medical.vn', N'Khoa Tai Mũi Họng', N'Viêm xoang, viêm họng', N'Bác sĩ chuyên điều trị viêm xoang và viêm họng kéo dài.', 7, 4.4),
    ('bs.voanhtuan@medical.vn', N'Khoa Xương Khớp', N'Cơ xương khớp', N'Bác sĩ chuyên điều trị đau khớp, thoái hóa khớp và bệnh cơ xương.', 13, 4.7),
    ('bs.nguyengiabao@medical.vn', N'Khoa Xương Khớp', N'Chấn thương chỉnh hình', N'Bác sĩ chuyên tư vấn và điều trị chấn thương chỉnh hình.', 8, 4.5)
) AS v(email, department_name, specialization, bio, experience_years, rating)
JOIN dbo.users u ON u.email = v.email
JOIN dbo.departments dep ON dep.name = v.department_name;
GO

INSERT INTO dbo.patients(user_id, date_of_birth, gender, address, blood_type)
SELECT u.id, v.date_of_birth, v.gender, v.address, v.blood_type
FROM (VALUES
    ('duc.pham@gmail.com', CAST('1990-05-15' AS DATE), 'MALE', N'123 Lê Lợi, Quận 1, TP.HCM', 'O+'),
    ('em.hoang@gmail.com', CAST('1995-11-20' AS DATE), 'FEMALE', N'456 Nguyễn Huệ, Quận 3, TP.HCM', 'A+'),
    ('nam.nguyen@gmail.com', CAST('1988-03-08' AS DATE), 'MALE', N'789 Cầu Giấy, Hà Nội', 'B+')
) AS v(email, date_of_birth, gender, address, blood_type)
JOIN dbo.users u ON u.email = v.email;
GO

-- Create future working schedules.
;WITH days AS (
    SELECT 1 AS day_offset UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
    UNION ALL SELECT 13 UNION ALL SELECT 14
),
doctor_list AS (
    SELECT d.id AS doctor_id, ROW_NUMBER() OVER (ORDER BY d.id) AS rn
    FROM dbo.doctors d
)
INSERT INTO dbo.work_schedules(doctor_id, work_date, shift, available)
SELECT
    dl.doctor_id,
    DATEADD(DAY, days.day_offset, CAST(GETDATE() AS DATE)),
    CASE WHEN dl.rn % 2 = 1 THEN 'MORNING' ELSE 'AFTERNOON' END,
    1
FROM doctor_list dl
CROSS JOIN days
WHERE
    (dl.rn % 2 = 1 AND days.day_offset IN (1, 3, 5, 8, 10, 12, 14))
    OR
    (dl.rn % 2 = 0 AND days.day_offset IN (2, 4, 6, 9, 11, 13));
GO

-- Create default time slots for each schedule.
;WITH slot_templates AS (
    SELECT 'MORNING' AS shift, CAST('08:00:00' AS TIME(0)) AS start_time, CAST('09:00:00' AS TIME(0)) AS end_time
    UNION ALL SELECT 'MORNING', CAST('09:00:00' AS TIME(0)), CAST('10:00:00' AS TIME(0))
    UNION ALL SELECT 'MORNING', CAST('10:00:00' AS TIME(0)), CAST('11:00:00' AS TIME(0))
    UNION ALL SELECT 'AFTERNOON', CAST('14:00:00' AS TIME(0)), CAST('15:00:00' AS TIME(0))
    UNION ALL SELECT 'AFTERNOON', CAST('15:00:00' AS TIME(0)), CAST('16:00:00' AS TIME(0))
    UNION ALL SELECT 'AFTERNOON', CAST('16:00:00' AS TIME(0)), CAST('17:00:00' AS TIME(0))
)
INSERT INTO dbo.time_slots(schedule_id, start_time, end_time, booked_capacity, max_capacity, status)
SELECT
    ws.id,
    st.start_time,
    st.end_time,
    0,
    3,
    CASE WHEN ws.available = 1 THEN 'AVAILABLE' ELSE 'UNAVAILABLE' END
FROM dbo.work_schedules ws
JOIN slot_templates st ON st.shift = ws.shift;
GO

-- Sample appointments.
DECLARE @patient1 BIGINT = (SELECT id FROM dbo.patients WHERE user_id = (SELECT id FROM dbo.users WHERE email = 'duc.pham@gmail.com'));
DECLARE @patient2 BIGINT = (SELECT id FROM dbo.patients WHERE user_id = (SELECT id FROM dbo.users WHERE email = 'em.hoang@gmail.com'));
DECLARE @patient3 BIGINT = (SELECT id FROM dbo.patients WHERE user_id = (SELECT id FROM dbo.users WHERE email = 'nam.nguyen@gmail.com'));

DECLARE @slot1 BIGINT = (
    SELECT TOP 1 ts.id
    FROM dbo.time_slots ts
    JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
    JOIN dbo.doctors d ON d.id = ws.doctor_id
    JOIN dbo.users u ON u.id = d.user_id
    WHERE u.email = 'bs.nguyenvanan@medical.vn'
      AND ts.start_time = '08:00:00'
    ORDER BY ws.work_date
);

DECLARE @slot2 BIGINT = (
    SELECT TOP 1 ts.id
    FROM dbo.time_slots ts
    JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
    JOIN dbo.doctors d ON d.id = ws.doctor_id
    JOIN dbo.users u ON u.id = d.user_id
    WHERE u.email = 'bs.vuhaiyen@medical.vn'
      AND ts.start_time = '14:00:00'
    ORDER BY ws.work_date
);

INSERT INTO dbo.appointments(patient_id, doctor_id, time_slot_id, status, notes)
SELECT @patient1, ws.doctor_id, @slot1, 'CONFIRMED', N'Đau ngực nhẹ, cần kiểm tra tim mạch'
FROM dbo.time_slots ts
JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
WHERE ts.id = @slot1;

INSERT INTO dbo.appointments(patient_id, doctor_id, time_slot_id, status, notes)
SELECT @patient2, ws.doctor_id, @slot2, 'PENDING', N'Da nổi mẩn đỏ, ngứa kéo dài'
FROM dbo.time_slots ts
JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
WHERE ts.id = @slot2;

INSERT INTO dbo.appointments(patient_id, doctor_id, time_slot_id, status, notes)
SELECT @patient3, ws.doctor_id, @slot2, 'PENDING', N'Tái khám da liễu'
FROM dbo.time_slots ts
JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
WHERE ts.id = @slot2;
GO

-- Sync booked capacity after sample appointments.
UPDATE ts
SET ts.booked_capacity = booked.count_booked
FROM dbo.time_slots ts
CROSS APPLY (
    SELECT COUNT(*) AS count_booked
    FROM dbo.appointments a
    WHERE a.time_slot_id = ts.id
      AND a.status <> 'CANCELLED'
) booked;

UPDATE dbo.time_slots
SET status =
    CASE
        WHEN status = 'UNAVAILABLE' THEN 'UNAVAILABLE'
        WHEN booked_capacity >= max_capacity THEN 'FULL'
        ELSE 'AVAILABLE'
    END;
GO

-- Sample completed medical record.
DECLARE @completedAppointment BIGINT = (
    SELECT TOP 1 id
    FROM dbo.appointments
    WHERE status = 'CONFIRMED'
    ORDER BY id
);

DECLARE @recordDoctor BIGINT = (SELECT doctor_id FROM dbo.appointments WHERE id = @completedAppointment);
DECLARE @recordPatient BIGINT = (SELECT patient_id FROM dbo.appointments WHERE id = @completedAppointment);

INSERT INTO dbo.medical_records(appointment_id, doctor_id, patient_id, diagnosis, treatment, notes)
VALUES (
    @completedAppointment,
    @recordDoctor,
    @recordPatient,
    N'Đau ngực cần theo dõi, chưa ghi nhận dấu hiệu cấp cứu.',
    N'Nghỉ ngơi, theo dõi huyết áp, tái khám nếu triệu chứng tăng.',
    N'Khuyến nghị kiểm tra lại sau 7 ngày.'
);

DECLARE @medicalRecordId BIGINT = SCOPE_IDENTITY();

INSERT INTO dbo.prescriptions(medical_record_id, medicine_name, dosage, duration_days, instructions)
VALUES
    (@medicalRecordId, N'Paracetamol 500mg', N'1 viên/lần, tối đa 3 lần/ngày', 3, N'Uống sau ăn khi đau hoặc sốt.'),
    (@medicalRecordId, N'Vitamin C 1000mg', N'1 viên/ngày', 7, N'Uống sau ăn sáng.');
GO

-- Sample upload.
INSERT INTO dbo.file_uploads(uploaded_by_user_id, appointment_id, patient_id, file_name, file_url, file_type, description)
SELECT
    u.id,
    a.id,
    a.patient_id,
    N'ket-qua-xet-nghiem-mau.pdf',
    N'/uploads/ket-qua-xet-nghiem-mau.pdf',
    'TEST_RESULT',
    N'Kết quả xét nghiệm máu trước buổi khám'
FROM dbo.appointments a
JOIN dbo.patients p ON p.id = a.patient_id
JOIN dbo.users u ON u.id = p.user_id
WHERE a.id = (SELECT TOP 1 id FROM dbo.appointments ORDER BY id);
GO

-- ============================================================
--  5. VIEWS
-- ============================================================

CREATE VIEW dbo.vw_appointment_details AS
SELECT
    a.id AS appointment_id,
    ws.work_date AS appointment_date,
    ts.start_time,
    ts.end_time,
    a.status,
    a.notes AS appointment_notes,
    a.created_at,
    ts.booked_capacity,
    ts.max_capacity,
    ts.status AS time_slot_status,

    pu.full_name AS patient_name,
    pu.email AS patient_email,
    pu.phone AS patient_phone,
    p.gender,
    p.date_of_birth,
    p.blood_type,

    du.full_name AS doctor_name,
    du.email AS doctor_email,
    doc.specialization,
    doc.rating,
    dep.name AS department_name
FROM dbo.appointments a
JOIN dbo.patients p ON p.id = a.patient_id
JOIN dbo.users pu ON pu.id = p.user_id
JOIN dbo.doctors doc ON doc.id = a.doctor_id
JOIN dbo.users du ON du.id = doc.user_id
JOIN dbo.time_slots ts ON ts.id = a.time_slot_id
JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
LEFT JOIN dbo.departments dep ON dep.id = doc.department_id;
GO

CREATE VIEW dbo.vw_available_time_slots AS
SELECT
    ts.id AS time_slot_id,
    ws.id AS schedule_id,
    ws.work_date,
    ws.shift,
    ts.start_time,
    ts.end_time,
    ts.booked_capacity,
    ts.max_capacity,
    ts.status,
    du.full_name AS doctor_name,
    doc.id AS doctor_id,
    doc.specialization,
    dep.id AS department_id,
    dep.name AS department_name
FROM dbo.time_slots ts
JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
JOIN dbo.doctors doc ON doc.id = ws.doctor_id
JOIN dbo.users du ON du.id = doc.user_id
LEFT JOIN dbo.departments dep ON dep.id = doc.department_id
WHERE ws.available = 1
  AND ws.work_date >= CAST(GETDATE() AS DATE)
  AND ts.status = 'AVAILABLE'
  AND ts.booked_capacity < ts.max_capacity;
GO

CREATE VIEW dbo.vw_medical_records_full AS
SELECT
    mr.id AS record_id,
    mr.created_at,
    mr.diagnosis,
    mr.treatment,
    mr.notes,
    ws.work_date AS appointment_date,
    ts.start_time,
    ts.end_time,
    pu.full_name AS patient_name,
    p.date_of_birth,
    p.gender,
    p.blood_type,
    du.full_name AS doctor_name,
    doc.specialization,
    dep.name AS department_name
FROM dbo.medical_records mr
JOIN dbo.appointments a ON a.id = mr.appointment_id
JOIN dbo.time_slots ts ON ts.id = a.time_slot_id
JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
JOIN dbo.patients p ON p.id = mr.patient_id
JOIN dbo.users pu ON pu.id = p.user_id
JOIN dbo.doctors doc ON doc.id = mr.doctor_id
JOIN dbo.users du ON du.id = doc.user_id
LEFT JOIN dbo.departments dep ON dep.id = doc.department_id;
GO

-- ============================================================
--  6. STORED PROCEDURES
-- ============================================================

CREATE PROCEDURE dbo.sp_book_appointment
    @patient_id BIGINT,
    @time_slot_id BIGINT,
    @notes NVARCHAR(MAX) = NULL,
    @new_id BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRANSACTION;

    DECLARE @doctor_id BIGINT;
    DECLARE @booked INT;
    DECLARE @max INT;
    DECLARE @slot_status NVARCHAR(20);

    SELECT
        @doctor_id = ws.doctor_id,
        @booked = ts.booked_capacity,
        @max = ts.max_capacity,
        @slot_status = ts.status
    FROM dbo.time_slots ts WITH (UPDLOCK, ROWLOCK)
    JOIN dbo.work_schedules ws ON ws.id = ts.schedule_id
    WHERE ts.id = @time_slot_id
      AND ws.available = 1;

    IF @doctor_id IS NULL
    BEGIN
        RAISERROR(N'Không tìm thấy khung giờ khám.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF @slot_status <> 'AVAILABLE' OR @booked >= @max
    BEGIN
        RAISERROR(N'Khung giờ này đã đầy hoặc không khả dụng.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    IF EXISTS (
        SELECT 1
        FROM dbo.appointments
        WHERE patient_id = @patient_id
          AND time_slot_id = @time_slot_id
          AND status <> 'CANCELLED'
    )
    BEGIN
        RAISERROR(N'Bệnh nhân đã đặt khung giờ này.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    INSERT INTO dbo.appointments(patient_id, doctor_id, time_slot_id, status, notes)
    VALUES (@patient_id, @doctor_id, @time_slot_id, 'PENDING', @notes);

    SET @new_id = SCOPE_IDENTITY();

    UPDATE dbo.time_slots
    SET
        booked_capacity = booked_capacity + 1,
        status = CASE
            WHEN booked_capacity + 1 >= max_capacity THEN 'FULL'
            ELSE 'AVAILABLE'
        END
    WHERE id = @time_slot_id;

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.sp_cancel_appointment
    @appointment_id BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRANSACTION;

    DECLARE @time_slot_id BIGINT;

    SELECT @time_slot_id = time_slot_id
    FROM dbo.appointments
    WHERE id = @appointment_id
      AND status <> 'CANCELLED';

    IF @time_slot_id IS NULL
    BEGIN
        RAISERROR(N'Không tìm thấy lịch hẹn cần hủy.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    UPDATE dbo.appointments
    SET status = 'CANCELLED'
    WHERE id = @appointment_id;

    UPDATE dbo.time_slots
    SET
        booked_capacity = CASE WHEN booked_capacity > 0 THEN booked_capacity - 1 ELSE 0 END,
        status = CASE
            WHEN status <> 'UNAVAILABLE' THEN 'AVAILABLE'
            ELSE 'UNAVAILABLE'
        END
    WHERE id = @time_slot_id;

    COMMIT TRANSACTION;
END;
GO

CREATE PROCEDURE dbo.sp_dashboard_stats
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        (SELECT COUNT(*) FROM dbo.appointments WHERE CAST(created_at AS DATE) = CAST(GETDATE() AS DATE)) AS appointments_today,
        (SELECT COUNT(*) FROM dbo.appointments WHERE status = 'PENDING') AS pending_appointments,
        (SELECT COUNT(*) FROM dbo.appointments WHERE status = 'CONFIRMED') AS confirmed_appointments,
        (SELECT COUNT(*) FROM dbo.appointments WHERE status = 'COMPLETED') AS completed_appointments,
        (SELECT COUNT(*) FROM dbo.patients) AS total_patients,
        (SELECT COUNT(*) FROM dbo.doctors) AS total_doctors,
        (SELECT COUNT(*) FROM dbo.departments) AS total_departments,
        (SELECT COUNT(*) FROM dbo.medical_records) AS total_medical_records,
        (SELECT COUNT(*) FROM dbo.time_slots WHERE status = 'AVAILABLE') AS available_time_slots,
        (SELECT COUNT(*) FROM dbo.time_slots WHERE status = 'FULL') AS full_time_slots;
END;
GO

-- ============================================================
--  7. QUICK CHECK
-- ============================================================

SELECT * FROM dbo.departments ORDER BY id;
SELECT * FROM dbo.vw_available_time_slots ORDER BY work_date, doctor_name, start_time;
SELECT * FROM dbo.vw_appointment_details ORDER BY appointment_id;
EXEC dbo.sp_dashboard_stats;
GO

PRINT N'MedicalAppointmentSystem rebuild completed successfully.';
GO
