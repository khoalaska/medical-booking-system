package fpt.medical.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medical_records")
@Getter
@Setter
@NoArgsConstructor
@Builder
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String diagnosis;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String treatment;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Prescription> prescriptions = new ArrayList<>();
}
