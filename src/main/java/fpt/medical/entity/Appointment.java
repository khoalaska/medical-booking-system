package fpt.medical.entity;

import fpt.medical.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL)
    private MedicalRecord medicalRecord;
}
